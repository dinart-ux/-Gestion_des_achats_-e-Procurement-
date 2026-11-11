package com.example.backend;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Persistence.PasswordResetTokenEntity;
import com.example.backend.Persistence.UtilisateurAuthStateEntity;
import com.example.backend.Repository.PasswordResetTokenRepository;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.Repository.UtilisateurAuthStateRepository;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Service.PasswordResetService;
import com.example.backend.Service.PasswordResetEmailService;
import com.example.backend.Service.PasswordResetTokenService;
import com.example.backend.Service.UtilisateurAuthStateService;
import com.example.backend.security.JwtUtil;
import com.example.backend.security.WebSocketAuthInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:password-reset-it;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.auth-state.enabled=true",
        "jwt.secret=test-only-secret-with-more-than-32-bytes"
})
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UtilisateurRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired UtilisateurAuthStateRepository states;
    @Autowired PasswordResetTokenService tokenService;
    @Autowired PasswordResetService resetService;
    @Autowired UtilisateurAuthStateService authState;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @Autowired WebSocketAuthInterceptor wsInterceptor;
    @MockitoBean com.example.backend.Service.AchatWorkflowService workflow;
    @MockitoBean com.example.backend.Service.FournisseurService fournisseurs;
    @MockitoBean PasswordResetEmailService resetEmail;

    private Integer userId;

    @BeforeEach
    void prepareUser() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS utilisateur_auth_state (utilisateur_id INTEGER PRIMARY KEY, token_version INTEGER NOT NULL DEFAULT 0)");
        String roleCode = "RESET_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO role(code,libelle) VALUES (?,?)", roleCode, "Test reset");
        Integer roleId = jdbc.queryForObject("SELECT id FROM role WHERE code = ?", Integer.class, roleCode);
        Utilisateur user = new Utilisateur();
        user.setMatricule("R" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        user.setNom("Test");
        user.setPrenom("Reset");
        user.setEmail(user.getMatricule().toLowerCase() + "@example.test");
        user.setPassword(encoder.encode("Old-pass-123"));
        user.setRole(roleId);
        userId = users.saveAndFlush(user).getId();
        UtilisateurAuthStateEntity state = new UtilisateurAuthStateEntity();
        state.setUtilisateurId(userId);
        states.saveAndFlush(state);
    }

    @Test
    void resetConsommeLeJetonChangeLeMotDePasseEtRevoqueLeJwt() {
        String oldJwt = jwt.generateToken(users.findById(userId).orElseThrow().getMatricule(), 0);
        String token = tokenService.createToken(userId);
        assertTrue(tokenService.isValid(token), "Le jeton fraîchement créé doit pouvoir être validé avec son verrou pessimiste");

        assertTrue(TransactionSynchronizationManager.isActualTransactionActive() == false,
                "Le test appelle le bean proxifié hors de sa transaction");
        resetService.resetPassword(token, "New-pass-456");

        assertTrue(encoder.matches("New-pass-456", users.findById(userId).orElseThrow().getPassword()));
        assertEquals(1, states.findById(userId).orElseThrow().getTokenVersion());
        assertFalse(tokenService.isValid(token));
        assertEquals(0, jwt.extractAuthVersion(oldJwt));
        assertFalse(authState.isCurrentVersion(users.findById(userId).orElseThrow().getMatricule(),
                jwt.extractAuthVersion(oldJwt)));
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive() == false);
    }

    @Test
    void echecIncrementRollbackMotDePasseEtConsommation() {
        String token = tokenService.createToken(userId);
        jdbc.update("DELETE FROM utilisateur_auth_state WHERE utilisateur_id = ?", userId);

        assertThrows(IllegalStateException.class, () -> resetService.resetPassword(token, "New-pass-456"));

        assertTrue(encoder.matches("Old-pass-123", users.findById(userId).orElseThrow().getPassword()));
        assertTrue(tokenService.isValid(token));
        assertTrue(states.findById(userId).isEmpty());
    }

    @Test
    void deuxConsommationsSequentiellesNePeuventUtiliserLeMemeJeton() {
        String token = tokenService.createToken(userId);
        resetService.resetPassword(token, "New-pass-456");
        assertThrows(IllegalArgumentException.class, () -> resetService.resetPassword(token, "Third-pass-789"));
        assertTrue(encoder.matches("New-pass-456", users.findById(userId).orElseThrow().getPassword()));
    }

    @Test
    void serviceDeResetEstUnProxyTransactionnel() {
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(resetService));
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(tokenService));
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(authState));
    }

    @Test
    void forgotRenvoieUneReponseGeneriqueEtEnvoieAuCompteCorrespondant() throws Exception {
        Utilisateur user = users.findById(userId).orElseThrow();
        MvcResult existing = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v2/auth/password/forgot").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"" + user.getMatricule() + "\",\"email\":\"" + user.getEmail() + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        MvcResult absent = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v2/auth/password/forgot").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"MISSING-" + java.util.UUID.randomUUID() + "\",\"email\":\"" + user.getEmail() + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
        assertEquals(existing.getResponse().getContentAsString(), absent.getResponse().getContentAsString());
        verify(resetEmail).sendResetLink(eq(user.getEmail()), anyString());
    }

    @Test
    void forgotNeRevelePasUnCompteInactifOuAbsent() throws Exception {
        Utilisateur user = users.findById(userId).orElseThrow();
        String bodyInactive = "{\"matricule\":\"" + user.getMatricule() + "\",\"email\":\"" + user.getEmail() + "\"}";
        user.setActif(false);
        users.saveAndFlush(user);

        MvcResult inactive = mvc.perform(post("/api/v2/auth/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(bodyInactive))
                .andExpect(status().isOk()).andReturn();
        MvcResult absent = mvc.perform(post("/api/v2/auth/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"MISSING-" + java.util.UUID.randomUUID() + "\",\"email\":\"" + user.getEmail() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertEquals(absent.getResponse().getContentAsString(), inactive.getResponse().getContentAsString());
        verifyNoInteractions(resetEmail);
    }

    @Test
    void forgotNEnvoieRienSiLeMatriculeEtLeMailNeCorrespondentPas() throws Exception {
        Utilisateur user = users.findById(userId).orElseThrow();
        mvc.perform(post("/api/v2/auth/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"" + user.getMatricule() + "\",\"email\":\"autre@example.test\"}"))
                .andExpect(status().isOk());
        verifyNoInteractions(resetEmail);
    }

    @Test
    void echecSmtpConserveLaReponseGenerique() throws Exception {
        Utilisateur user = users.findById(userId).orElseThrow();
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(resetEmail).sendResetLink(eq(user.getEmail()), anyString());

        mvc.perform(post("/api/v2/auth/password/forgot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"matricule\":\"" + user.getMatricule() + "\",\"email\":\"" + user.getEmail() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Si les informations correspondent à un compte actif, vous recevrez un lien de récupération par e-mail."));
    }

    @Test
    void changementPersonnelVerifieLeMotDePasseEtRevoqueLeJwt() throws Exception {
        String matricule = users.findById(userId).orElseThrow().getMatricule();
        String currentJwt = jwt.generateToken(matricule, 0);
        String path = "/api/v2/auth/password/change";

        mvc.perform(post(path).header("Authorization", "Bearer " + currentJwt)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"New-pass-456\",\"confirmPassword\":\"New-pass-456\"}"))
                .andExpect(status().isUnauthorized());
        assertTrue(encoder.matches("Old-pass-123", users.findById(userId).orElseThrow().getPassword()));

        mvc.perform(post(path).header("Authorization", "Bearer " + currentJwt)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Old-pass-123\",\"newPassword\":\"New-pass-456\",\"confirmPassword\":\"New-pass-456\"}"))
                .andExpect(status().isOk());
        assertTrue(encoder.matches("New-pass-456", users.findById(userId).orElseThrow().getPassword()));
        assertEquals(1, states.findById(userId).orElseThrow().getTokenVersion());
        mvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer " + currentJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointAdministrateurEstProtege() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v2/auth/password/admin/MISSING/forgot"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void ancienJwtEstRefuseSurRouteHttpProtegeeApresReset() throws Exception {
        String matricule = users.findById(userId).orElseThrow().getMatricule();
        String oldJwt = jwt.generateToken(matricule, 0);
        resetService.resetPassword(tokenService.createToken(userId), "New-pass-456");

        mvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tableAuthStateInaccessibleEntraineUnRefusHttp() throws Exception {
        String matricule = users.findById(userId).orElseThrow().getMatricule();
        String jwtV0 = jwt.generateToken(matricule, 0);
        jdbc.execute("DROP TABLE utilisateur_auth_state");

        mvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer " + jwtV0))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void websocketRefuseAncienJwtEtAccepteUneReconnexionAvecLeNouveau() {
        String matricule = users.findById(userId).orElseThrow().getMatricule();
        String oldJwt = jwt.generateToken(matricule, 0);
        resetService.resetPassword(tokenService.createToken(userId), "New-pass-456");
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> wsInterceptor.preSend(connect(oldJwt), mock(MessageChannel.class)));

        String newJwt = jwt.generateToken(matricule,
                authState.requireAndGetVersion(userId));
        Message<?> accepted = wsInterceptor.preSend(connect(newJwt), mock(MessageChannel.class));
        assertNotNull(StompHeaderAccessor.wrap(accepted).getUser());
    }

    private Message<byte[]> connect(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

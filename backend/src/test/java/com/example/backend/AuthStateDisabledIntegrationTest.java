package com.example.backend;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-disabled-it;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.auth-state.enabled=false",
        "jwt.secret=test-only-secret-with-more-than-32-bytes"
})
@AutoConfigureMockMvc
class AuthStateDisabledIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UtilisateurRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtil jwt;
    @Autowired MockMvc mvc;
    @MockitoBean com.example.backend.Service.AchatWorkflowService workflow;
    @MockitoBean com.example.backend.Service.FournisseurService fournisseurs;

    @Test
    void loginEtAncienJwtFonctionnentSansTableAuthState() throws Exception {
        jdbc.update("INSERT INTO role(code,libelle) VALUES (?,?)", "ADMIN", "Test admin");
        Integer roleId = jdbc.queryForObject("SELECT id FROM role WHERE code='ADMIN'", Integer.class);
        String matricule = "D" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Utilisateur user = new Utilisateur();
        user.setMatricule(matricule);
        user.setNom("Test");
        user.setPrenom("Disabled");
        user.setEmail(matricule.toLowerCase() + "@example.test");
        user.setPassword(encoder.encode("Old-pass-123"));
        user.setRole(roleId);
        users.saveAndFlush(user);
        jdbc.execute("DROP TABLE utilisateur_auth_state");

        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"identifiant\":\"" + matricule + "\",\"password\":\"Old-pass-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        String legacyJwt = jwt.generateToken(matricule);
        mvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer " + legacyJwt))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Referrer-Policy", "no-referrer"));
        assertEquals(0, jwt.extractAuthVersion(legacyJwt));
        assertTrue(jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_name='UTILISATEUR_AUTH_STATE'").isEmpty());
    }
}

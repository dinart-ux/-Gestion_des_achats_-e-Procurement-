package com.example.backend.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchatWorkflowSoumissionTest {
    @Mock private JdbcTemplate jdbc;
    @Mock private NotificationService notificationService;
    @Mock private WorkflowEventService workflowEventService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private UtilisateurAuthStateService authStateService;
    private AchatWorkflowService service;

    @BeforeEach void setUpService() {
        service = new AchatWorkflowService(jdbc, notificationService, workflowEventService,
                passwordEncoder, authStateService, false);
    }

    @Test
    void soumissionInsereLeManagerEnPremierMemeSiSonSeuilDepasseLeMontant() {
        preparerSoumissionValide();

        service.soumettre(5, "demandeur@example.test");

        ArgumentCaptor<String> requetes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametres = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, Mockito.times(4)).update(requetes.capture(), parametres.capture());
        assertTrue(requetes.getAllValues().get(0).contains("INSERT INTO public.validation"));
        assertEquals(21, parametres.getAllValues().get(0)[1]);
        assertEquals(1, parametres.getAllValues().get(0)[2]);
        assertEquals(22, parametres.getAllValues().get(1)[1]);
        assertEquals(1, parametres.getAllValues().get(1)[2]);
        assertTrue(requetes.getAllValues().get(2).contains("UPDATE public.demande_achat"));
        verify(jdbc).queryForList(org.mockito.ArgumentMatchers.contains("n.seuil_montant<=? OR n.id=?"),
                eq(Integer.class), any(Object[].class));
    }

    @Test
    void soumissionEstTransactionnelle() throws Exception {
        assertTrue(AchatWorkflowService.class.getMethod("soumettre", long.class, String.class)
                .isAnnotationPresent(Transactional.class));
    }

    @Test
    void resoumissionApresPrecisionCreeLaTentativeSuivanteSansEffacerLesAnciennesDecisions() {
        preparerSoumissionValide(1, 1, 10);

        service.soumettre(5, "demandeur@example.test");

        ArgumentCaptor<String> requetes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parametres = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc, Mockito.times(4)).update(requetes.capture(), parametres.capture());
        assertTrue(requetes.getAllValues().get(0).contains("tentative"));
        assertEquals(2, parametres.getAllValues().get(0)[2]);
        assertEquals(2, parametres.getAllValues().get(1)[2]);
        verify(jdbc, never()).update(org.mockito.ArgumentMatchers.contains("UPDATE public.validation"), any(Object[].class));
        verify(jdbc, never()).update(org.mockito.ArgumentMatchers.contains("DELETE FROM public.validation"), any(Object[].class));
    }

    @Test
    void nouvelleTentativeEstRefuseeSiLaPrecedenteNeComportePasDeDemandeDePrecision() {
        preparerSoumissionValide(1, 0, 10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("demande de précision"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void utilisateurQuiNestPasProprietaireNePeutPasCreerUneTentative() {
        preparerSoumissionValide(1, 1, 99);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void uneDemandeRendueEnBrouillonApresPrecisionConserveSonHistoriqueEtNePeutPasEtreSupprimee() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(10));
        when(jdbc.queryForMap(anyString(), any(Object[].class)))
                .thenReturn(Map.of("statut", "BROUILLON", "demandeur", 10));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.supprimerBrouillon(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("historique de validation"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void endpointDeSoumissionEstReserveAuxDemandeurs() throws Exception {
        var method = com.example.backend.Controlleur.AchatWorkflowController.class
                .getMethod("soumettreDemande", long.class, java.security.Principal.class);
        var restriction = method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertEquals("hasRole('DEMANDEUR')", restriction.value());
    }

    @Test
    void seulesLesValidationsDeLaTentativeActiveSontEnAttenteEtLesAnciennesRestentConsultables() {
        List<String> requetes = new ArrayList<>();
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(10));
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            requetes.add(sql);
            return sql.contains("SELECT id") && sql.contains("public.utilisateurs") ? List.of(10) : List.of();
        });

        service.validationsEnAttente("demandeur@example.test", "MANAGER", null, null, null, null);
        service.validationsTraitees("demandeur@example.test", "MANAGER", null, null, null, null);

        String attente = requetes.stream().filter(sql -> sql.contains("v.decision='EN_ATTENTE'")).findFirst().orElseThrow();
        String traitees = requetes.stream().filter(sql -> sql.contains("tentative_active")).findFirst().orElseThrow();
        assertTrue(attente.contains("v.tentative=(SELECT MAX(active.tentative)"));
        assertTrue(attente.contains("p.tentative=v.tentative"));
        assertTrue(traitees.contains("v.tentative AS tentative"));
        assertTrue(traitees.contains("OR v.tentative <"));
    }

    @Test
    void circuitSansNiveauManagerEstRefuseAvantTouteEcriture() {
        preparerSoumissionValide();
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.contains("SELECT n.id"), eq(Integer.class), any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("aucun niveau MANAGER"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void demandeIncompleteEstRefuseeAvantLaCreationDesValidations() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(List.of(10));
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of(
                "statut", "BROUILLON", "demandeur", 10, "objet", "", "centre_cout_id", 3));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("objet et le centre"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void ligneIncompleteEstRefuseeAvantLaCreationDesValidations() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(List.of(10));
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of(
                "statut", "BROUILLON", "demandeur", 10, "objet", "Achat de test", "centre_cout_id", 3));
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("Complétez chaque ligne"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void managerInvalideEstRefuseAvantLaCreationDesValidations() {
        preparerSoumissionValide();
        when(jdbc.queryForObject(org.mockito.ArgumentMatchers.contains("SELECT EXISTS"), eq(Boolean.class), any(Object[].class)))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.soumettre(5, "demandeur@example.test"));

        assertTrue(exception.getMessage().contains("Manager associé"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void preparerSoumissionValide() {
        preparerSoumissionValide(0, 0, 10);
    }

    private void preparerSoumissionValide(int derniereTentative, int precisionsDemandees, int demandeurId) {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("FROM public.utilisateurs")) return List.of(10);
                    if (sql.contains("FROM public.niveau_validation")) return List.of(21, 22);
                    return List.of();
                });
        Mockito.lenient().when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("SELECT d.statut AS statut")) return Map.of("statut", "BROUILLON", "demandeur", demandeurId,
                    "objet", "Achat de test", "centre_cout_id", 3, "reference", "DA-TEST", "matricule", "demandeur");
            if (sql.contains("SELECT statut AS statut")) return Map.of("statut", "BROUILLON", "demandeur", demandeurId);
            if (sql.contains("SELECT manager_id, service_id")) return Map.of("manager_id", 11, "service_id", 8);
            if (sql.contains("SELECT d.id AS id")) return new java.util.HashMap<>(Map.of("id", 5, "statut", "SOUMISE"));
            throw new AssertionError("Requête inattendue: " + sql);
        });
        Mockito.lenient().when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0);
                    if (sql.contains("count(*) FROM public.ligne_demande l")) return 0;
                    if (sql.contains("count(*) FROM public.ligne_demande")) return 1;
                    if (sql.contains("COALESCE(MAX(tentative)")) return derniereTentative;
                    if (sql.contains("decision='PRECISION_DEMANDEE'")) return precisionsDemandees;
                    if (sql.contains("SELECT centre_cout_id")) return 3;
                    if (sql.contains("SELECT service_id FROM public.centre_cout")) return 8;
                    if (sql.contains("SELECT id FROM public.circuit_validation")) return 12;
                    if (sql.contains("SELECT n.id")) return 21;
                    throw new AssertionError("Requête inattendue: " + sql);
                });
        Mockito.lenient().when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class))).thenReturn(true);
        Mockito.lenient().when(jdbc.queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class)))
                .thenReturn(new BigDecimal("50"));
    }
}

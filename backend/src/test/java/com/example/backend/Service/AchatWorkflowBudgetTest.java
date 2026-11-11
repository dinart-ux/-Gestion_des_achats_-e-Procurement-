package com.example.backend.Service;

import com.example.backend.DTO.AchatRequests;
import com.example.backend.Controlleur.AchatWorkflowController;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchatWorkflowBudgetTest {
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
    void consultationCalculeSoldePourcentageEtAlerteAvecGardeSurZero() {
        when(jdbc.queryForList(contains("SELECT id"), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(42));
        when(jdbc.queryForList(contains("SELECT c.id AS centre_cout_id"), any(Object[].class)))
                .thenReturn(List.of(Map.of("centre_cout_id", 3, "solde_disponible", 25,
                        "consommation_pct", 75, "alerte_seuil", true)));

        List<Map<String, Object>> budgets = service.budgets(2035, "manager@example.test", "MANAGER");

        assertEquals(25, budgets.get(0).get("solde_disponible"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("montant_alloue - b.montant_engage AS solde_disponible"));
        assertTrue(sql.getValue().contains("CASE WHEN b.montant_alloue>0"));
        assertTrue(sql.getValue().contains("b.montant_engage*100/b.montant_alloue>=b.seuil_alerte_pct"));
    }

    @Test
    void consultationAccepteUnFiltreCentreEtConserveLeFiltreManager() {
        when(jdbc.queryForList(contains("SELECT id"), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(42));
        when(jdbc.queryForList(contains("SELECT c.id AS centre_cout_id"), any(Object[].class)))
                .thenReturn(List.of());

        service.budgets(2035, 3, "manager@example.test", "MANAGER");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("CAST(? AS INTEGER) IS NULL OR c.id=?"));
        assertTrue(sql.getValue().contains("s.id=(SELECT u.service_id"));
    }

    @Test
    void listeDeCentresPourBudgetNeRetourneQueLesCentresActifs() {
        service.centresCoutActifsPourBudget();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture());
        assertTrue(sql.getValue().contains("WHERE actif=true"));
        assertTrue(sql.getValue().contains("code, libelle"));
    }

    @Test
    void demandeurNePeutPasConsulterLesBudgets() {
        assertThrows(AccessDeniedException.class,
                () -> service.budgets(2035, "demandeur@example.test", "DEMANDEUR"));
        verify(jdbc, never()).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void enregistrementUtiliseLaContrainteUniqueEtConserveLeMontantEngage() {
        when(jdbc.queryForList(contains("WHERE id=? AND actif=true"),
                eq(Integer.class), any(Object[].class))).thenReturn(List.of(3));
        when(jdbc.queryForMap(contains("SELECT b.id AS budget_id"), any(Object[].class)))
                .thenReturn(Map.of("budget_id", 7, "montant_engage", 125));

        service.enregistrerBudget(new AchatRequests.Budget(3, 2035,
                new BigDecimal("1000.00"), new BigDecimal("80.00")));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), any(Object[].class));
        assertTrue(sql.getValue().contains("ON CONFLICT (centre_cout_id, annee) DO UPDATE"));
        assertTrue(sql.getValue().contains("SET montant_alloue=EXCLUDED.montant_alloue"));
        assertTrue(!sql.getValue().contains("montant_engage=EXCLUDED"));
    }

    @Test
    void centreInexistantNeDeclencheAucuneEcriture() {
        when(jdbc.queryForList(contains("WHERE id=? AND actif=true"),
                eq(Integer.class), any(Object[].class))).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.enregistrerBudget(new AchatRequests.Budget(999, 2035,
                        BigDecimal.TEN, new BigDecimal("80"))));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void enregistrementVerifieQueLeCentreEstActif() {
        when(jdbc.queryForList(contains("actif=true"), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(3));
        when(jdbc.queryForMap(contains("SELECT b.id AS budget_id"), any(Object[].class)))
                .thenReturn(Map.of("budget_id", 7));

        service.enregistrerBudget(new AchatRequests.Budget(3, 2035,
                BigDecimal.TEN, new BigDecimal("80")));

        verify(jdbc).queryForList(contains("WHERE id=? AND actif=true"), eq(Integer.class), any(Object[].class));
    }

    @Test
    void validationDTOAccepteUnBudgetValideEtRefuseMontantSeuilEtAnneeInvalides() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new AchatRequests.Budget(3, 2026,
                    new BigDecimal("1000.00"), new BigDecimal("80.00"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 2026,
                    BigDecimal.ZERO, new BigDecimal("80.00"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 2026,
                    new BigDecimal("-1.00"), new BigDecimal("80.00"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 2026,
                    BigDecimal.TEN, new BigDecimal("-0.01"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 2026,
                    BigDecimal.TEN, new BigDecimal("100.01"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 1999,
                    BigDecimal.TEN, new BigDecimal("80.00"))).isEmpty());
            assertTrue(!validator.validate(new AchatRequests.Budget(3, 2101,
                    BigDecimal.TEN, new BigDecimal("80.00"))).isEmpty());
        }
    }

    @Test
    void ecritureBudgetEstReserveeAuxAdministrateursParLAPI() throws Exception {
        Method endpoint = AchatWorkflowController.class.getMethod(
                "enregistrerBudget", AchatRequests.Budget.class);
        PreAuthorize authorization = endpoint.getAnnotation(PreAuthorize.class);

        Assertions.assertNotNull(authorization);
        assertEquals("hasRole('ADMIN')", authorization.value());
    }
}

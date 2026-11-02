package com.example.backend.Service;

import com.example.backend.DTO.AchatRequests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;

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
class AchatWorkflowOffreTest {
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private AchatWorkflowService service;

    @Test
    void selectionNePeutPasReactiverUneOffreRefusee() {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("statut", "VALIDEE")))
                .thenReturn(List.of(Map.of("id", 14, "statut", "REFUSEE")));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.retenirOffre(12, 14));

        assertEquals(409, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void refusNePeutPasAnnulerUneOffreDejaRetenue() {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("statut", "VALIDEE")))
                .thenReturn(List.of(Map.of("id", 14, "statut", "RETENUE")));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.refuserOffre(12, 14));

        assertEquals(409, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void aucuneNouvelleOffreApresLaSelectionDuFournisseur() {
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("statut", "VALIDEE")));
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), any(Object[].class)))
                .thenReturn(false, true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.creerOffre(12, new AchatRequests.Offre(8, java.math.BigDecimal.TEN,
                        5, "paiement à 30 jours")));

        assertEquals(409, exception.getStatusCode().value());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void validationsEnAttenteFiltreLesDemandesParManagerDesigne() {
        when(jdbc.queryForList(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(List.of(42));
        when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("demande_id", 7)));

        var results = service.validationsEnAttente("manager@example.test", "MANAGER",
                null, null, null, null);

        assertEquals(1, results.size());
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).queryForList(query.capture(), parameters.capture());
        assertTrue(query.getValue().contains("u.manager_id=?"));
        assertEquals("MANAGER", parameters.getValue()[0]);
        assertEquals(42, parameters.getValue()[4]);
    }
}

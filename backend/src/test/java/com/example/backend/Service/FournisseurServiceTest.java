package com.example.backend.Service;

import com.example.backend.DTO.AchatRequests;
import com.example.backend.Persistence.CategorieEntity;
import com.example.backend.Persistence.FournisseurCategorieEntity;
import com.example.backend.Persistence.FournisseurEntity;
import com.example.backend.Repository.CategorieRepository;
import com.example.backend.Repository.FournisseurCategorieRepository;
import com.example.backend.Repository.FournisseurRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FournisseurServiceTest {
    @Mock private FournisseurRepository fournisseurs;
    @Mock private FournisseurCategorieRepository fournisseurCategories;
    @Mock private CategorieRepository categories;
    @Mock private JdbcTemplate jdbc;
    @InjectMocks private FournisseurService service;

    @Test
    void creationEnregistreLesCoordonneesEtLesCategoriesSansDoublon() {
        AchatRequests.Fournisseur request = request(" Atelier Nord ", List.of(3, 3));
        CategorieEntity category = new CategorieEntity();
        category.setId(3);
        when(categories.findAllById(List.of(3))).thenReturn(List.of(category));
        when(fournisseurs.save(any())).thenAnswer(invocation -> {
            FournisseurEntity saved = invocation.getArgument(0);
            saved.setId(21);
            return saved;
        });
        when(fournisseurCategories.findAllByFournisseurId(21)).thenReturn(List.of());

        var response = service.create(request);

        ArgumentCaptor<FournisseurEntity> supplierCaptor = ArgumentCaptor.forClass(FournisseurEntity.class);
        verify(fournisseurs).save(supplierCaptor.capture());
        assertEquals("Atelier Nord", supplierCaptor.getValue().getRaisonSociale());
        assertEquals("achats@atelier.test", supplierCaptor.getValue().getEmail());
        assertEquals("30 jours", supplierCaptor.getValue().getConditionsPaiement());

        ArgumentCaptor<FournisseurCategorieEntity> relationCaptor =
                ArgumentCaptor.forClass(FournisseurCategorieEntity.class);
        verify(fournisseurCategories).save(relationCaptor.capture());
        assertEquals(21, relationCaptor.getValue().getFournisseurId());
        assertEquals(3, relationCaptor.getValue().getCategorieId());
        assertEquals("Atelier Nord", response.get("raison_sociale"));
    }

    @Test
    void creationRefuseUneRaisonSocialeDejaPresente() {
        when(fournisseurs.existsByRaisonSocialeIgnoreCase("Atelier Nord")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request("Atelier Nord", List.of())));

        assertEquals(409, exception.getStatusCode().value());
        verify(fournisseurs, never()).save(any());
    }

    @Test
    void modificationRemplaceLesCategoriesDansLaMemeTransaction() {
        FournisseurEntity current = new FournisseurEntity();
        current.setId(21);
        current.setRaisonSociale("Atelier Nord");
        CategorieEntity category = new CategorieEntity();
        category.setId(5);
        when(fournisseurs.findById(21)).thenReturn(Optional.of(current));
        when(categories.findAllById(List.of(5))).thenReturn(List.of(category));
        when(fournisseurs.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fournisseurCategories.findAllByFournisseurId(21)).thenReturn(List.of());

        service.update(21, request("Atelier Nord", List.of(5)));

        verify(fournisseurCategories).deleteAllByFournisseurId(21);
        verify(fournisseurCategories).save(any(FournisseurCategorieEntity.class));
        assertEquals("achats@atelier.test", current.getEmail());
    }

    private AchatRequests.Fournisseur request(String name, List<Integer> categoryIds) {
        return new AchatRequests.Fournisseur(name, "NIF-1", "STAT-1", "Antananarivo",
                "0200000000", "achats@atelier.test", "Mme Rabe", "30 jours", 7, categoryIds);
    }
}

package com.example.backend.Service;

import com.example.backend.Models.Produit;
import com.example.backend.Repository.ProduitRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProduitServiceTest {
    private final ProduitRepository produits = mock(ProduitRepository.class);
    private final ProduitService service = new ProduitService(produits);

    @Test
    void listerRetourneLesProduitsSousFormeDeDto() {
        when(produits.findAll()).thenReturn(List.of(
                new Produit("Papier", "A4", "Fournitures", new BigDecimal("4.25"), 20)));

        var resultat = service.getAll();

        assertEquals(1, resultat.size());
        assertEquals("Papier", resultat.get(0).getNom());
        assertEquals(new BigDecimal("4.25"), resultat.get(0).getPrixUnitaire());
    }

    @Test
    void supprimerDelegueAuRepository() {
        service.delete(12L);

        verify(produits).deleteById(12L);
    }
}

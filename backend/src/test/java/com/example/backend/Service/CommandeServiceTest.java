package com.example.backend.Service;

import com.example.backend.Models.Commande;
import com.example.backend.Models.Demande;
import com.example.backend.Models.DemandeItem;
import com.example.backend.Models.Produit;
import com.example.backend.Repository.CommandeRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandeServiceTest {
    private final CommandeRepository commandes = mock(CommandeRepository.class);
    private final CommandeService service = new CommandeService(commandes);

    @Test
    void creerDepuisUneDemandeCalculeLeTotalEtRelieLesLignes() {
        Produit produit = new Produit("Clavier", "", "Informatique", new BigDecimal("12.50"), 10);
        DemandeItem ligne = new DemandeItem();
        ligne.setProduit(produit);
        ligne.setQuantite(3);
        ligne.setPrixUnitaire(new BigDecimal("12.50"));
        Demande demande = new Demande();
        demande.setItems(List.of(ligne));

        when(commandes.save(any(Commande.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Commande resultat = service.createFromDemande(demande);

        assertEquals(new BigDecimal("37.50"), resultat.getMontantTotal());
        assertEquals(1, resultat.getItems().size());
        assertEquals(resultat, resultat.getItems().get(0).getCommande());
        assertEquals(produit, resultat.getItems().get(0).getProduit());
        verify(commandes).save(resultat);
    }
}

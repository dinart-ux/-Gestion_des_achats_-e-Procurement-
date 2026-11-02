package com.example.backend.Service;

import com.example.backend.Models.Demande;
import com.example.backend.Models.DemandeItem;
import com.example.backend.Repository.DemandeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemandeServiceTest {
    private final DemandeRepository demandes = mock(DemandeRepository.class);
    private final CommandeService commandes = mock(CommandeService.class);
    private final DemandeService service = new DemandeService(demandes, commandes);

    @Test
    void sauvegarderRelieChaqueLigneALaDemande() {
        Demande demande = new Demande();
        DemandeItem ligne = new DemandeItem();
        demande.setItems(List.of(ligne));
        when(demandes.save(demande)).thenReturn(demande);

        Demande resultat = service.save(demande);

        assertSame(demande, resultat);
        assertSame(demande, ligne.getDemande());
        verify(demandes).save(demande);
    }

    @Test
    void soumettreChangeLeStatutEtPersisteLaDemande() {
        Demande demande = new Demande();
        when(demandes.findById(7L)).thenReturn(Optional.of(demande));
        when(demandes.save(any(Demande.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Demande resultat = service.submit(7L);

        assertEquals("SOUMISE", resultat.getStatut());
        verify(demandes).save(demande);
    }
}

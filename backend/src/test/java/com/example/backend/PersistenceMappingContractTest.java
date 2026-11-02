package com.example.backend;

import com.example.backend.Persistence.CategorieEntity;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Models.Commande;
import com.example.backend.Models.CommandeItem;
import com.example.backend.Models.Demande;
import com.example.backend.Models.DemandeItem;
import com.example.backend.Models.Offre;
import com.example.backend.Models.Produit;
import com.example.backend.Persistence.ValidationEntity;
import com.example.backend.Repository.CommandeRepository;
import com.example.backend.Repository.DemandeRepository;
import com.example.backend.Repository.ProduitRepository;
import com.example.backend.Service.CommandeService;
import com.example.backend.Service.DemandeService;
import com.example.backend.Service.ProduitService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Service;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PersistenceMappingContractTest {

    @Test
    void utilisateurEntityCibleLaTablePlurielleReelle() {
        assertEquals("\"utilisateurs\"", Utilisateur.class.getAnnotation(Table.class).name());
    }

    @Test
    void validationEntityReflèteLaTentativeEtSonUnicite() throws Exception {
        Table table = ValidationEntity.class.getAnnotation(Table.class);
        UniqueConstraint unique = table.uniqueConstraints()[0];

        assertEquals("validation", table.name());
        assertArrayEquals(new String[]{"demande_id", "niveau_id", "tentative"}, unique.columnNames());
        assertNotNull(ValidationEntity.class.getDeclaredField("tentative")
                .getAnnotation(Column.class));
    }

    @Test
    void categorieExposeLeStatutActifPresentEnBase() throws Exception {
        Column actif = CategorieEntity.class.getDeclaredField("actif").getAnnotation(Column.class);

        assertNotNull(actif);
        assertEquals(false, actif.nullable());
    }

    @Test
    void modelesEtServicesHeritesNeSontPasEnregistresCommePersistanceActive() {
        for (Class<?> type : Arrays.asList(Commande.class, CommandeItem.class, Demande.class,
                DemandeItem.class, Offre.class, Produit.class)) {
            assertNull(type.getAnnotation(Entity.class), type.getSimpleName());
        }
        for (Class<?> type : Arrays.asList(CommandeService.class, DemandeService.class, ProduitService.class)) {
            assertNull(type.getAnnotation(Service.class), type.getSimpleName());
        }
        assertNotNull(CommandeRepository.class.getAnnotation(NoRepositoryBean.class));
        assertNotNull(DemandeRepository.class.getAnnotation(NoRepositoryBean.class));
        assertNotNull(ProduitRepository.class.getAnnotation(NoRepositoryBean.class));
    }
}

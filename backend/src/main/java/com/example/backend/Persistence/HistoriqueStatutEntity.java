package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "historique_statut") @Getter @Setter @NoArgsConstructor
public class HistoriqueStatutEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "demande_id", nullable = false) private Integer demandeId;
    @Column(name = "ancien_statut", length = 20) private String ancienStatut;
    @Column(name = "nouveau_statut", nullable = false, length = 20) private String nouveauStatut;
    @Column(name = "utilisateur_id") private Integer utilisateurId;
    @Column(length = 255) private String commentaire;
    @Column(name = "date_changement", nullable = false) private LocalDateTime dateChangement;
}

package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "fournisseur") @Getter @Setter @NoArgsConstructor
public class FournisseurEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "raison_sociale", nullable = false, length = 150) private String raisonSociale;
    @Column(length = 30) private String nif;
    @Column(length = 30) private String stat;
    @Column(length = 255) private String adresse;
    @Column(length = 30) private String telephone;
    @Column(length = 150) private String email;
    @Column(name = "contact_nom", length = 100) private String contactNom;
    @Column(name = "conditions_paiement", length = 255) private String conditionsPaiement;
    @Column(name = "delai_livraison_jours") private Integer delaiLivraisonJours;
    @Column(nullable = false) private boolean actif = true;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation;
}

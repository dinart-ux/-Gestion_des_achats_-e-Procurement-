package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "offre", uniqueConstraints = @UniqueConstraint(columnNames = {"demande_id", "fournisseur_id"})) @Getter @Setter @NoArgsConstructor
public class OffreEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "demande_id", nullable = false) private Integer demandeId;
    @Column(name = "fournisseur_id", nullable = false) private Integer fournisseurId;
    @Column(name = "montant_total", nullable = false, precision = 15, scale = 2) private BigDecimal montantTotal;
    @Column(name = "delai_livraison_jours") private Integer delaiLivraisonJours;
    @Column(length = 255) private String conditions;
    @Column(nullable = false, length = 20) private String statut;
    @Column(name = "date_offre", nullable = false) private LocalDate dateOffre;
}

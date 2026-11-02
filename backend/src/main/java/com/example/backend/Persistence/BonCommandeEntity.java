package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "bon_commande") @Getter @Setter @NoArgsConstructor
public class BonCommandeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true, length = 30) private String numero;
    @Column(name = "demande_id", nullable = false, unique = true) private Integer demandeId;
    @Column(name = "offre_id") private Integer offreId;
    @Column(name = "fournisseur_id", nullable = false) private Integer fournisseurId;
    @Column(name = "cree_par_id", nullable = false) private Integer creeParId;
    @Column(name = "montant_total", nullable = false, precision = 15, scale = 2) private BigDecimal montantTotal;
    @Column(nullable = false, length = 30) private String statut;
    @Column(name = "date_emission", nullable = false) private LocalDateTime dateEmission;
    @Column(name = "date_livraison_prevue") private LocalDate dateLivraisonPrevue;
}

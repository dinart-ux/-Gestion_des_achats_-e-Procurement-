package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "ligne_bon_commande") @Getter @Setter @NoArgsConstructor
public class LigneBonCommandeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "bon_commande_id", nullable = false) private Integer bonCommandeId;
    @Column(name = "ligne_demande_id") private Integer ligneDemandeId;
    @Column(nullable = false, length = 200) private String designation;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantite;
    @Column(name = "prix_unitaire", nullable = false, precision = 15, scale = 2) private BigDecimal prixUnitaire;
}

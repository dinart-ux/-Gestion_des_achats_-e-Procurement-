package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "ligne_demande") @Getter @Setter @NoArgsConstructor
public class LigneDemandeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "demande_id", nullable = false) private Integer demandeId;
    @Column(name = "categorie_id") private Integer categorieId;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, length = 200) private String designation;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal quantite;
    @Column(name = "prix_unitaire_estime", nullable = false, precision = 15, scale = 2) private BigDecimal prixUnitaireEstime;
}

package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "demande_achat") @Getter @Setter @NoArgsConstructor
public class DemandeAchatEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true, length = 30) private String reference;
    @Column(name = "demandeur_id", nullable = false) private Integer demandeurId;
    @Column(name = "centre_cout_id", nullable = false) private Integer centreCoutId;
    @Column(nullable = false, length = 200) private String objet;
    @Column(columnDefinition = "text") private String justification;
    @Column(name = "montant_estime", nullable = false, precision = 15, scale = 2) private BigDecimal montantEstime;
    @Column(nullable = false, length = 20) private String statut;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation;
    @Column(name = "date_soumission") private LocalDateTime dateSoumission;
}

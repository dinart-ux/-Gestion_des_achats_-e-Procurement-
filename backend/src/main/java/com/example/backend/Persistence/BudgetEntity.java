package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "budget", uniqueConstraints = @UniqueConstraint(columnNames = {"centre_cout_id", "annee"})) @Getter @Setter @NoArgsConstructor
public class BudgetEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "centre_cout_id", nullable = false) private Integer centreCoutId;
    @Column(nullable = false) private Integer annee;
    @Column(name = "montant_alloue", nullable = false, precision = 15, scale = 2) private BigDecimal montantAlloue;
    @Column(name = "montant_engage", nullable = false, precision = 15, scale = 2) private BigDecimal montantEngage;
    @Column(name = "seuil_alerte_pct", nullable = false, precision = 5, scale = 2) private BigDecimal seuilAlertePct;
}

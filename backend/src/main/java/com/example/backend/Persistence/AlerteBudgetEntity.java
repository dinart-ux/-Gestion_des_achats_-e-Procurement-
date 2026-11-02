package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "alerte_budget") @Getter @Setter @NoArgsConstructor
public class AlerteBudgetEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "budget_id", nullable = false) private Integer budgetId;
    @Column(name = "type_alerte", nullable = false, length = 20) private String typeAlerte;
    @Column(nullable = false, length = 255) private String message;
    @Column(name = "date_alerte", nullable = false) private LocalDateTime dateAlerte;
    @Column(nullable = false) private boolean traitee;
}

package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "niveau_validation", uniqueConstraints = @UniqueConstraint(columnNames = {"circuit_id", "ordre"})) @Getter @Setter @NoArgsConstructor
public class NiveauValidationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "circuit_id", nullable = false) private Integer circuitId;
    @Column(nullable = false) private Integer ordre;
    @Column(name = "role_id", nullable = false) private Integer roleId;
    @Column(name = "seuil_montant", nullable = false, precision = 15, scale = 2) private BigDecimal seuilMontant;
}

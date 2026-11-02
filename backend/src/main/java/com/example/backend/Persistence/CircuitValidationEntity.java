package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "circuit_validation") @Getter @Setter @NoArgsConstructor
public class CircuitValidationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, length = 100) private String nom;
    @Column(name = "service_id") private Integer serviceId;
    @Column(nullable = false) private boolean actif = true;
}

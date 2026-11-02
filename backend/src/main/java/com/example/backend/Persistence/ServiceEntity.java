package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "service") @Getter @Setter @NoArgsConstructor
public class ServiceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true, length = 20) private String code;
    @Column(nullable = false, length = 100) private String nom;
    @Column(nullable = false) private boolean actif = true;
}

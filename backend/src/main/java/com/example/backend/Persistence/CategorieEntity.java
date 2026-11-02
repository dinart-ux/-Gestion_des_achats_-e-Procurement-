package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "categorie") @Getter @Setter @NoArgsConstructor
public class CategorieEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true, length = 100) private String nom;
    @Column(length = 255) private String description;
    @Column(nullable = false) private boolean actif = true;
}

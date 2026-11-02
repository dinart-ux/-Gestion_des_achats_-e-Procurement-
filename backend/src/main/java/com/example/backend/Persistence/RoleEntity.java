package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "role") @Getter @Setter @NoArgsConstructor
public class RoleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(nullable = false, unique = true, length = 30) private String code;
    @Column(nullable = false, length = 100) private String libelle;
}

package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

// Kept only as a legacy schema bean. The active JPA mapping is Models.Utilisateur.
@Getter @Setter @NoArgsConstructor
public class UtilisateurEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(unique = true, length = 20) private String matricule;
    @Column(nullable = false, length = 100) private String nom;
    @Column(nullable = false, length = 100) private String prenom;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(name = "mot_de_passe", nullable = false, length = 255) private String motDePasse;
    @Column(length = 30) private String telephone;
    @Column(name = "role_id", nullable = false) private Integer roleId;
    @Column(name = "service_id") private Integer serviceId;
    @Column(name = "manager_id") private Integer managerId;
    @Column(nullable = false) private boolean actif = true;
    @Column(name = "date_creation", nullable = false) private LocalDateTime dateCreation;
}

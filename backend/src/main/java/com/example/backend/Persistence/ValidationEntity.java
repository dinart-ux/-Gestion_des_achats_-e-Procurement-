package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "validation", uniqueConstraints = @UniqueConstraint(columnNames = {"demande_id", "niveau_id", "tentative"})) @Getter @Setter @NoArgsConstructor
public class ValidationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "demande_id", nullable = false) private Integer demandeId;
    @Column(name = "niveau_id", nullable = false) private Integer niveauId;
    @Column(nullable = false) private Integer tentative = 1;
    @Column(name = "valideur_id") private Integer valideurId;
    @Column(nullable = false, length = 30) private String decision;
    @Column(columnDefinition = "text") private String commentaire;
    @Column(name = "date_decision") private LocalDateTime dateDecision;
}

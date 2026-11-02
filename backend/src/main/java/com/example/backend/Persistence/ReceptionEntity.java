package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Table(name = "reception") @Getter @Setter @NoArgsConstructor
public class ReceptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "bon_commande_id", nullable = false) private Integer bonCommandeId;
    @Column(name = "receptionnaire_id", nullable = false) private Integer receptionnaireId;
    @Column(name = "date_reception", nullable = false) private LocalDateTime dateReception;
    @Column(name = "type_reception", nullable = false, length = 10) private String typeReception;
    @Column(length = 255) private String observation;
}

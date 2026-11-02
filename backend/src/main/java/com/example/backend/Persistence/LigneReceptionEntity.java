package com.example.backend.Persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "ligne_reception") @Getter @Setter @NoArgsConstructor
public class LigneReceptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "reception_id", nullable = false) private Integer receptionId;
    @Column(name = "ligne_bc_id", nullable = false) private Integer ligneBcId;
    @Column(name = "quantite_recue", nullable = false, precision = 12, scale = 2) private BigDecimal quantiteRecue;
}

package com.example.backend.Persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "fournisseur_categorie") @IdClass(FournisseurCategorieId.class)
@Getter @Setter @NoArgsConstructor
public class FournisseurCategorieEntity {
    @Id @Column(name = "fournisseur_id") private Integer fournisseurId;
    @Id @Column(name = "categorie_id") private Integer categorieId;
}

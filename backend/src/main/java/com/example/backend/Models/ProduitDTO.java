package com.example.backend.Models;

import java.math.BigDecimal;

public class ProduitDTO {
    private Long id;
    private String nom;
    private String categorie;
    private BigDecimal prixUnitaire;
    private Integer stock;

    public ProduitDTO(Long id, String nom, String categorie, BigDecimal prixUnitaire, Integer stock) {
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.prixUnitaire = prixUnitaire;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}

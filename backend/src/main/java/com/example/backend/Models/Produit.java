package com.example.backend.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Legacy model retained for isolated unit tests; it is not a mapped entity. */
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private String categorie;

    @Column(nullable = false)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean actif = true;

    public Produit() {
    }

    public Produit(String nom, String description, String categorie, BigDecimal prixUnitaire, Integer stock) {
        this.nom = nom;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}

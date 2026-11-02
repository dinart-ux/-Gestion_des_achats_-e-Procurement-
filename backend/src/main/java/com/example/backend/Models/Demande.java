package com.example.backend.Models;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Legacy model retained for isolated unit tests; it is not a mapped entity. */
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    @JsonIgnore
    private Utilisateur createur;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(nullable = false)
    private String statut = "BROUILLON";

    @Column(nullable = true)
    private BigDecimal montantEstime = BigDecimal.ZERO;

    @Column(length = 2000)
    private String justification;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandeItem> items = new ArrayList<>();

    public Demande() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Utilisateur getCreateur() {
        return createur;
    }

    public void setCreateur(Utilisateur createur) {
        this.createur = createur;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public BigDecimal getMontantEstime() {
        return montantEstime;
    }

    public void setMontantEstime(BigDecimal montantEstime) {
        this.montantEstime = montantEstime;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public List<DemandeItem> getItems() {
        return items;
    }

    public void setItems(List<DemandeItem> items) {
        this.items = items;
    }
}

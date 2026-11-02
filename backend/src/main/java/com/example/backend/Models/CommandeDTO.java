package com.example.backend.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommandeDTO {
    private Long id;
    private LocalDateTime dateCommande;
    private String statut;
    private BigDecimal montantTotal;

    public CommandeDTO(Long id, LocalDateTime dateCommande, String statut, BigDecimal montantTotal) {
        this.id = id;
        this.dateCommande = dateCommande;
        this.statut = statut;
        this.montantTotal = montantTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDateTime dateCommande) {
        this.dateCommande = dateCommande;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }
}

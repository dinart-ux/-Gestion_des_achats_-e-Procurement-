package com.example.backend.Models;
import jakarta.persistence.*;

/** Legacy model retained for isolated unit tests; the active workflow uses public.offre. */
public class Offre {

    @Id
    @Column(name = "\"Num_Offre\"")
    private String numOffre;

    @Column(name = "\"Prix_Offre\"")
    private Integer prixOffre;

    @Column(name = "\"Condition_Offre\"")
    private String ConditionOffre;

    @Column(name = "\"Num_Commande\"")
    private Integer numCommande;

    @Column(name = "\"Ref_Fournisseur\"")
    private String refFournisseur;

    public Offre() {
    }

    public Offre( String numOffre, Integer prixOffre, String condition_Offre, Integer numCommande, String refFournisseur) {
        this.numOffre = numOffre;
        this.prixOffre = prixOffre;
        this.ConditionOffre = condition_Offre;
        this.numCommande = numCommande;
        this.refFournisseur = refFournisseur;
    }

    // Getters et Setters
    public String getNumOffre() {
        return numOffre;
    }
    public void setNumOffre(String numOffre) {
        this.numOffre = numOffre;
    }
    public Integer getPrixOffre() {
        return prixOffre;
    }
    public void setPrixOffre(Integer prixOffre) {
        this.prixOffre = prixOffre;
    }
    public String getConditionOffre() {
        return ConditionOffre;
    }
    public void setConditionOffre(String ConditionOffre) {
        ConditionOffre = ConditionOffre;
    }
    public Integer getNumCommande() {
        return numCommande;
    }
    public void setNumCommande(Integer numCommande) {
        this.numCommande = numCommande;
    }
    public String getRefFournisseur() {
        return refFournisseur;
    }
    public void setRefFournisseur(String refFournisseur) {
        this.refFournisseur = refFournisseur;
    }

}

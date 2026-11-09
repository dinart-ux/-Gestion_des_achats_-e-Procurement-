package com.example.backend.Persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Version d'authentification indépendante de la table utilisateur existante. */
@Entity
@Table(name = "utilisateur_auth_state")
public class UtilisateurAuthStateEntity {

    @Id
    @Column(name = "utilisateur_id", nullable = false)
    private Integer utilisateurId;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    public Integer getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Integer utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }
}

package com.example.backend.Persistence;

import java.io.Serializable;
import java.util.Objects;

public class FournisseurCategorieId implements Serializable {
    private Integer fournisseurId;
    private Integer categorieId;

    public FournisseurCategorieId() {}
    public FournisseurCategorieId(Integer fournisseurId, Integer categorieId) {
        this.fournisseurId = fournisseurId;
        this.categorieId = categorieId;
    }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FournisseurCategorieId that)) return false;
        return Objects.equals(fournisseurId, that.fournisseurId) && Objects.equals(categorieId, that.categorieId);
    }
    @Override public int hashCode() { return Objects.hash(fournisseurId, categorieId); }
}

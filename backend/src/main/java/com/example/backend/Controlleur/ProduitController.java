package com.example.backend.Controlleur;

import com.example.backend.Models.Produit;
import com.example.backend.Models.ProduitDTO;
import com.example.backend.Service.ProduitService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Deprecated(forRemoval = true)
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(ProduitService produitService) {
        this.produitService = produitService;
    }

    @GetMapping("/produits")
    public List<ProduitDTO> getAll() {
        return produitService.getAll();
    }

    @GetMapping("/produits/{id}")
    public Produit getById(@PathVariable Long id) {
        return produitService.getById(id);
    }

    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    @PostMapping("/produits")
    public Produit create(@RequestBody Produit produit) {
        return produitService.save(produit);
    }

    @PreAuthorize("hasAnyRole('SERVICE_ACHATS', 'ADMIN')")
    @DeleteMapping("/produits/{id}")
    public void delete(@PathVariable Long id) {
        produitService.delete(id);
    }
}

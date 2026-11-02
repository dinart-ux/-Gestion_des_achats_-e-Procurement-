package com.example.backend.Service;

import com.example.backend.Models.Produit;
import com.example.backend.Models.ProduitDTO;
import com.example.backend.Repository.ProduitRepository;
import java.util.List;
import java.util.stream.Collectors;

public class ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitService(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    public List<ProduitDTO> getAll() {
        return produitRepository.findAll().stream()
                .map(p -> new ProduitDTO(p.getId(), p.getNom(), p.getCategorie(), p.getPrixUnitaire(), p.getStock()))
                .collect(Collectors.toList());
    }

    public Produit getById(Long id) {
        return produitRepository.findById(id).orElse(null);
    }

    public Produit save(Produit produit) {
        return produitRepository.save(produit);
    }

    public void delete(Long id) {
        produitRepository.deleteById(id);
    }
}

package com.example.backend.Repository;

import com.example.backend.Models.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
@NoRepositoryBean
public interface ProduitRepository extends JpaRepository<Produit, Long> {
}

package com.example.backend.Repository;

import com.example.backend.Models.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CommandeRepository extends JpaRepository<Commande, Long> {
}

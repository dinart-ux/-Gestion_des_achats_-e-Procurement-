package com.example.backend.Repository;

import com.example.backend.Models.Demande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;

@NoRepositoryBean
public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByStatut(String statut);
}

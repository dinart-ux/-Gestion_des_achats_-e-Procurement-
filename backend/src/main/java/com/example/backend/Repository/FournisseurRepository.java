package com.example.backend.Repository;

import com.example.backend.Persistence.FournisseurEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FournisseurRepository
        extends JpaRepository<FournisseurEntity, Integer> {
    java.util.List<FournisseurEntity> findAllByOrderByRaisonSocialeAsc();
    boolean existsByRaisonSocialeIgnoreCase(String raisonSociale);
    boolean existsByRaisonSocialeIgnoreCaseAndIdNot(String raisonSociale, Integer id);
}

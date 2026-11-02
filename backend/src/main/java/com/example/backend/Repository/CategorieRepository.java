package com.example.backend.Repository;

import com.example.backend.Persistence.CategorieEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface CategorieRepository
        extends JpaRepository<CategorieEntity, Integer> {
}


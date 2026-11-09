package com.example.backend.Repository;

import com.example.backend.Persistence.UtilisateurAuthStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurAuthStateRepository extends JpaRepository<UtilisateurAuthStateEntity, Integer> {
}

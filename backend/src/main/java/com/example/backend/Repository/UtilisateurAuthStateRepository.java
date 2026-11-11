package com.example.backend.Repository;

import com.example.backend.Persistence.UtilisateurAuthStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UtilisateurAuthStateRepository extends JpaRepository<UtilisateurAuthStateEntity, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE utilisateur_auth_state SET token_version = token_version + 1 WHERE utilisateur_id = :userId", nativeQuery = true)
    int incrementVersion(@Param("userId") Integer userId);
}

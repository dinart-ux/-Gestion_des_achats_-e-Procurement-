package com.example.backend.Repository;

import com.example.backend.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByMatricule(String matricule);
    Optional<Utilisateur> findByMatriculeIgnoreCaseAndActifTrue(String matricule);
    Optional<Utilisateur> findByMatriculeIgnoreCaseAndEmailIgnoreCaseAndActifTrue(String matricule, String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.id = :id")
    Optional<Utilisateur> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndMatriculeNot(String email, String matricule);
    boolean existsByMatricule(String matricule);
    @Query("select distinct u.role from Utilisateur u order by u.role")
    List<Integer> findDistinctRoleCodes();
}

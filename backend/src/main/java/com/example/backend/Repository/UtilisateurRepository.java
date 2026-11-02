package com.example.backend.Repository;

import com.example.backend.Models.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByMatricule(String matricule);
    Optional<Utilisateur> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndMatriculeNot(String email, String matricule);
    boolean existsByMatricule(String matricule);
    @Query("select distinct u.role from Utilisateur u order by u.role")
    List<Integer> findDistinctRoleCodes();
}

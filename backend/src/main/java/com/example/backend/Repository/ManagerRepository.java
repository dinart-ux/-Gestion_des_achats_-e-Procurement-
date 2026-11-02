package com.example.backend.Repository;

import com.example.backend.DTO.ManagerOption;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ManagerRepository {
    private final JdbcTemplate jdbc;

    public ManagerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean serviceExists(Integer serviceId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.service WHERE id = ?)",
                Boolean.class, serviceId);
        return Boolean.TRUE.equals(exists);
    }

    public List<ManagerOption> findActifsByService(Integer serviceId) {
        return jdbc.query("""
                SELECT u.id, u.matricule, u.nom, u.prenom
                FROM public.utilisateurs u
                JOIN public.role r ON r.id = u.role_id
                WHERE r.code = 'MANAGER' AND u.service_id = ? AND u.actif = TRUE
                ORDER BY u.nom, u.prenom, u.matricule
                """, (rs, rowNum) -> new ManagerOption(
                rs.getInt("id"), rs.getString("matricule"),
                rs.getString("nom"), rs.getString("prenom")), serviceId);
    }

    public boolean isActiveManagerOfService(Integer managerId, Integer serviceId) {
        Boolean valid = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.utilisateurs u
                    JOIN public.role r ON r.id = u.role_id
                    WHERE u.id = ? AND u.service_id = ? AND u.actif = TRUE
                      AND r.code = 'MANAGER'
                )
                """, Boolean.class, managerId, serviceId);
        return Boolean.TRUE.equals(valid);
    }
}

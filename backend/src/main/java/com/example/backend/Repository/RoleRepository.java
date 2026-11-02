package com.example.backend.Repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RoleRepository {

    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAllRoles() {
        return jdbc.queryForList("""
            SELECT id, code, libelle
            FROM public.role
            ORDER BY id
            """);
    }

    public Optional<Integer> findIdByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        List<Integer> ids = jdbc.query(
                """
                SELECT id
                FROM public.role
                WHERE UPPER(code) = UPPER(?)
                """,
                (rs, rowNum) -> rs.getInt("id"),
                code.trim()
        );

        return ids.stream().findFirst();
    }

    public Optional<String> findCodeById(Integer id) {
        if (id == null) return Optional.empty();
        return jdbc.query("SELECT code FROM public.role WHERE id = ?",
                rs -> rs.next() ? Optional.of(rs.getString("code")) : Optional.empty(), id);
    }
}

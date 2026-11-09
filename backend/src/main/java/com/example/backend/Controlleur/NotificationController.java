
package com.example.backend.Controlleur;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/notifications")
public class NotificationController {

    private final JdbcTemplate jdbc;

    public NotificationController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Récupérer les notifications non lues
    // de l'utilisateur connecté.
    @GetMapping("/non-lues")
    public ResponseEntity<List<Map<String, Object>>> getNotificationsNonLues(
            Principal principal) {

        List<Map<String, Object>> notifications = jdbc.queryForList("""
            SELECT
                n.id,
                n.message,
                n.type,
                n.reference,
                n.nouveau_statut,
                n.date_creation
            FROM public.notification n
            JOIN public.utilisateurs u
                ON u.id = n.utilisateur_id
            WHERE u.matricule = ?
              AND n.lu = FALSE
            ORDER BY n.date_creation ASC
            """, principal.getName());

        return ResponseEntity.ok(notifications);
    }

    // Marquer une notification comme lue.
    // Un utilisateur ne peut modifier que ses propres notifications.
    @PatchMapping("/{id}/lue")
    public ResponseEntity<Void> marquerCommeLue(
            @PathVariable Long id,
            Principal principal) {

        jdbc.update("""
            UPDATE public.notification n
            SET lu = TRUE
            FROM public.utilisateurs u
            WHERE n.utilisateur_id = u.id
              AND n.id = ?
              AND u.matricule = ?
            """, id, principal.getName());

        return ResponseEntity.noContent().build();
    }
}

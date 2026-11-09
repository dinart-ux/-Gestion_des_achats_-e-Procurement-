
package com.example.backend.Service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final JdbcTemplate jdbc;

    public NotificationService(
            SimpMessagingTemplate messagingTemplate,
            JdbcTemplate jdbc) {

        this.messagingTemplate = messagingTemplate;
        this.jdbc = jdbc;
    }

    public void notifierApresCommit(
            String matricule,
            Map<String, Object> notification) {

        if (matricule == null || matricule.isBlank()) {
            return;
        }

        // 1. Enregistrer la notification dans PostgreSQL.
        int inserted = jdbc.update("""
            INSERT INTO public.notification (
                utilisateur_id,
                message,
                type,
                reference,
                nouveau_statut
            )
            SELECT
                u.id,
                ?,
                ?,
                ?,
                ?
            FROM public.utilisateurs u
            WHERE u.matricule = ?
            """,
            notification.get("message"),
            notification.get("type"),
            notification.get("reference"),
            notification.get("nouveauStatut"),
            matricule
        );

        if (inserted == 0) {
            return;
        }

        // 2. Préparer l'envoi WebSocket après validation
        // définitive de la transaction.
        Runnable envoyer = () ->
                messagingTemplate.convertAndSendToUser(
                        matricule,
                        "/queue/notifications",
                        notification
                );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            envoyer.run();
                        }
                    }
            );

        } else {
            envoyer.run();
        }
    }
}

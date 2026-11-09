
package com.example.backend.Service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WorkflowEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public WorkflowEventService(
            SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publierApresCommit(
            String matricule,
            Map<String, Object> evenement) {

        if (matricule == null || matricule.isBlank()
                || evenement == null) {
            return;
        }

        Map<String, Object> donnees =
                new LinkedHashMap<>(evenement);

        Runnable envoyer = () ->
                messagingTemplate.convertAndSendToUser(
                        matricule,
                        "/queue/updates",
                        donnees
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

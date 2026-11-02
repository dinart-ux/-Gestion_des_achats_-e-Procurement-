package com.example.backend.Controlleur;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("Les données envoyées sont invalides.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody() {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Le corps de la requête est absent ou mal formé."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> authentication() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Identifiant ou mot de passe incorrect."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", safeMessage(exception, "Requête invalide.")));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Cette opération entre en conflit avec les données existantes."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Vous n’avez pas les droits nécessaires."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String reason = exception.getReason();
        String message = reason == null || reason.isBlank()
                ? status.getReasonPhrase()
                : reason;
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> internalError(Exception exception) {
        log.error("Erreur inattendue pendant le traitement d'une requête API", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Une erreur interne est survenue. Consultez les logs du serveur."));
    }

    private String safeMessage(Exception exception, String fallback) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }
}

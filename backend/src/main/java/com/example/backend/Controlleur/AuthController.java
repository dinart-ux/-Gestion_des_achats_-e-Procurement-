package com.example.backend.Controlleur;

import com.example.backend.DTO.LoginRequest;
import com.example.backend.DTO.LoginResponse;
import com.example.backend.Service.AuthService;
import com.example.backend.Service.PasswordResetEmailService;
import com.example.backend.Service.PasswordResetRateLimiter;
import com.example.backend.Service.PasswordResetService;
import com.example.backend.Service.PasswordResetTokenService;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.DTO.PasswordForgotRequest;
import com.example.backend.DTO.PasswordResetRequest;
import com.example.backend.DTO.PasswordChangeRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UtilisateurRepository users;
    private final PasswordResetTokenService tokens;
    private final PasswordResetEmailService emailService;
    private final PasswordResetRateLimiter limiter;
    private final PasswordResetService resetService;

    public AuthController(AuthService authService, UtilisateurRepository users,
            PasswordResetTokenService tokens, PasswordResetEmailService emailService,
            PasswordResetRateLimiter limiter, PasswordResetService resetService) {
        this.authService = authService;
        this.users = users;
        this.tokens = tokens;
        this.emailService = emailService;
        this.limiter = limiter;
        this.resetService = resetService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.connecter(request.identifiant(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/v2/auth/password/forgot")
    public ResponseEntity<java.util.Map<String, String>> forgot(@Valid @RequestBody PasswordForgotRequest request,
                                                               HttpServletRequest httpRequest) {
        String generic = "Si les informations correspondent à un compte actif, vous recevrez un lien de récupération par e-mail.";
        String ip = httpRequest.getRemoteAddr();
        String key = request.matricule().trim().toLowerCase() + ":" + ip;
        boolean allowed = limiter.allow(key) && limiter.allow("ip:" + ip);
        if (!allowed) return ResponseEntity.ok(java.util.Map.of("message", generic));
        users.findByMatriculeIgnoreCaseAndEmailIgnoreCaseAndActifTrue(
                request.matricule().trim(), request.email().trim()).ifPresent(user -> {
            try {
                String rawToken = tokens.createToken(user.getId());
                emailService.sendResetLink(user.getEmail(), rawToken);
            } catch (RuntimeException ignored) {
                // La réponse anti-énumération reste identique si la base ou SMTP est indisponible.
            }
        });
        return ResponseEntity.ok(java.util.Map.of("message", generic));
    }

    @org.springframework.web.bind.annotation.GetMapping("/v2/auth/password/validate")
    public ResponseEntity<java.util.Map<String, Boolean>> validateToken(
            @org.springframework.web.bind.annotation.RequestParam String token) {
        return ResponseEntity.ok(java.util.Map.of("valid", tokens.isValid(token)));
    }

    @PostMapping("/v2/auth/password/reset")
    public ResponseEntity<java.util.Map<String, String>> reset(@Valid @RequestBody PasswordResetRequest request) {
        validatePassword(request.newPassword(), request.confirmPassword());
        resetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(java.util.Map.of("message", "Votre mot de passe a été réinitialisé."));
    }

    @PostMapping("/v2/auth/password/change")
    public ResponseEntity<java.util.Map<String, String>> change(@Valid @RequestBody PasswordChangeRequest request,
                                                                Authentication authentication) {
        validatePassword(request.newPassword(), request.confirmPassword());
        resetService.changeOwnPassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(java.util.Map.of("message", "Votre mot de passe a été modifié."));
    }

    @PostMapping("/v2/auth/password/admin/{matricule}/forgot")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> adminForgot(@org.springframework.web.bind.annotation.PathVariable String matricule,
                                            HttpServletRequest request) {
        if (!limiter.allow("admin:" + request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de demandes.");
        }
        users.findByMatricule(matricule).filter(com.example.backend.Models.Utilisateur::isActif)
                .ifPresent(user -> {
                    try {
                        String token = tokens.createToken(user.getId());
                        emailService.sendResetLink(user.getEmail(), token);
                    } catch (RuntimeException ignored) { /* Secret absent des logs et de la réponse. */ }
                });
        return ResponseEntity.accepted().build();
    }

    private void validatePassword(String password, String confirmation) {
        if (!password.equals(confirmation)) throw new IllegalArgumentException("Les mots de passe ne correspondent pas.");
        if (password.length() < 12 || password.length() > 72
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 12 caractères, une majuscule, une minuscule, un chiffre et un symbole.");
        }
    }
}


package com.example.backend.security;

import com.example.backend.Service.UtilisateurAuthStateService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UtilisateurAuthStateService authStateService;

    public WebSocketAuthInterceptor(
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            UtilisateurAuthStateService authStateService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.authStateService = authStateService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        // Authentification lors de la connexion STOMP
        if (StompCommand.CONNECT.equals(command)) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {
                throw new AccessDeniedException("Token JWT manquant.");
            }

            String token = authorization.substring(7);

            if (!jwtUtil.isTokenValid(token)) {
                throw new AccessDeniedException(
                        "Token JWT invalide ou expiré.");
            }

            String matricule = jwtUtil.extractUsername(token);
            int authVersion = jwtUtil.extractAuthVersion(token);

            if (!authStateService.isCurrentVersion(matricule, authVersion)) {
                throw new AccessDeniedException(
                        "Token JWT révoqué.");
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(matricule);

            if (!userDetails.isEnabled()) {
                throw new AccessDeniedException(
                        "Compte utilisateur désactivé.");
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

            // Conserve l'identité dans le message STOMP original
            accessor.setUser(authentication);

            return message;
        }

        // Autoriser uniquement l'abonnement aux notifications privées
        if (StompCommand.SUBSCRIBE.equals(command)) {

            if (accessor.getUser() == null) {
                throw new AccessDeniedException(
                        "Utilisateur non authentifié.");
            }

            String destination = accessor.getDestination();

            if (!"/user/queue/notifications".equals(destination)
                && !"/user/queue/updates".equals(destination)) {
                throw new AccessDeniedException(
                        "Abonnement à cette destination interdit.");
            }

            return message;
        }

        // Les clients ne peuvent pas envoyer de messages au serveur
        if (StompCommand.SEND.equals(command)) {
            throw new AccessDeniedException(
                    "Envoi de messages client interdit.");
        }

        return message;
    }
}

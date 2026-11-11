package com.example.backend.security;

import com.example.backend.Service.UtilisateurAuthStateService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final UtilisateurAuthStateService authStateService = mock(UtilisateurAuthStateService.class);
    private final WebSocketAuthInterceptor interceptor =
            new WebSocketAuthInterceptor(jwtUtil, userDetailsService, authStateService, true);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void accepteUnConnectStompAvecUneVersionValide() {
        UserDetails user = User.withUsername("EMP-12")
                .password("ignored")
                .roles("DEMANDEUR")
                .build();
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("EMP-12");
        when(jwtUtil.extractAuthVersion("token")).thenReturn(0);
        when(authStateService.isCurrentVersion("EMP-12", 0)).thenReturn(true);
        when(userDetailsService.loadUserByUsername("EMP-12")).thenReturn(user);

        Message<?> result = interceptor.preSend(connectMessage("token"), channel);

        assertNotNull(StompHeaderAccessor.wrap(result).getUser());
    }

    @Test
    void refuseUnConnectStompQuandLaLigneDEtatEstAbsenteOuSaVersionEstRevoquee() {
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("EMP-12");
        when(jwtUtil.extractAuthVersion("token")).thenReturn(0);
        when(authStateService.isCurrentVersion("EMP-12", 0)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> interceptor.preSend(connectMessage("token"), channel));
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

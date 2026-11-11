package com.example.backend.security;

import com.example.backend.Service.UtilisateurAuthStateService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final UtilisateurAuthStateService authStateService = mock(UtilisateurAuthStateService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtUtil, userDetailsService, authStateService, true);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accepteUnJwtDontLaVersionCorrespondALETatCourant() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        UserDetails user = User.withUsername("EMP-12")
                .password("ignored")
                .roles("DEMANDEUR")
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("EMP-12");
        when(jwtUtil.extractAuthVersion("token")).thenReturn(2);
        when(authStateService.isCurrentVersion("EMP-12", 2)).thenReturn(true);
        when(userDetailsService.loadUserByUsername("EMP-12")).thenReturn(user);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejetteUnJwtQuandLaLigneDEtatEstAbsenteOuSaVersionEstRevoquee() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("EMP-12");
        when(jwtUtil.extractAuthVersion("token")).thenReturn(1);
        when(authStateService.isCurrentVersion("EMP-12", 1)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername("EMP-12");
        verify(chain).doFilter(request, response);
    }
}

package com.risheek.resume_screener.jwt;

import com.risheek.resume_screener.service.CustomUserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CustomUserDetailService customUserDetailService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void setUp() {
        jwtAuthFilter = new JwtAuthFilter(jwtUtil, customUserDetailService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_authEndpoint_skipsAuthenticationAndContinuesChain() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/auth/login");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(customUserDetailService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_headerWithoutBearerPrefix_continuesChainWithoutAuthentication() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Basic somecredentials");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_validToken_setsAuthenticationAndContinuesChain() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtUtil.extractUsername("validtoken")).thenReturn("user@example.com");

        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("irrelevant")
                .authorities("ROLE_USER")
                .build();
        when(customUserDetailService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("validtoken", "user@example.com")).thenReturn(true);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validUsernameButInvalidToken_doesNotSetAuthentication() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Bearer badtoken");
        when(jwtUtil.extractUsername("badtoken")).thenReturn("user@example.com");

        UserDetails userDetails = User.builder()
                .username("user@example.com")
                .password("irrelevant")
                .authorities("ROLE_USER")
                .build();
        when(customUserDetailService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("badtoken", "user@example.com")).thenReturn(false);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_expiredToken_returns401WithExpiredMessageAndStopsChain() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Bearer expiredtoken");
        when(jwtUtil.extractUsername("expiredtoken")).thenThrow(mock(ExpiredJwtException.class));

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Token expired"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_invalidToken_returns401WithInvalidMessageAndStopsChain() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Bearer malformedtoken");
        when(jwtUtil.extractUsername("malformedtoken")).thenThrow(new RuntimeException("bad token"));

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Invalid token"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_alreadyAuthenticated_doesNotReAuthenticate() throws Exception {
        when(request.getServletPath()).thenReturn("/api/v1/jobs");
        when(request.getHeader("Authorization")).thenReturn("Bearer validtoken");
        when(jwtUtil.extractUsername("validtoken")).thenReturn("user@example.com");

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken existingAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("already@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertEquals("already@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(customUserDetailService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
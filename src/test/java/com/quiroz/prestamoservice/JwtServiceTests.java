package com.quiroz.prestamoservice;

import com.quiroz.prestamoservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {

    private JwtService jwtService;
    private final String secretKey = "NGYyODcyZTBmYmJlNGFlOWFlOGM5MjdhNDZmM2FlYzM3OTYzYjkyYTQwNzQzZDUxNmVlYWRmMWFiODIyNGU4Ng==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
    }

    @Test
    @DisplayName("Generar token y validar subject y expiracion")
    void testGenerarYValidarToken() {
        String email = "prestamista@example.com";
        String token = jwtService.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedEmail = jwtService.extractUsername(token);
        assertEquals(email, extractedEmail);

        Date expiration = jwtService.extractExpiration(token);
        assertTrue(expiration.after(new Date()));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Validar token contra UserDetails correspondiente")
    void testTokenValidoConUserDetails() {
        String email = "prestamista@example.com";
        UserDetails userDetails = new User(email, "pass123", Collections.emptyList());

        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    @DisplayName("Token invalido si el subject no coincide")
    void testTokenInvalidoConOtroUsuario() {
        String token = jwtService.generateToken("usuario1@example.com");
        UserDetails otroUser = new User("usuario2@example.com", "pass123", Collections.emptyList());

        assertFalse(jwtService.isTokenValid(token, otroUser));
    }
}
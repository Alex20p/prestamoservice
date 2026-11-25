package com.quiroz.prestamoservice.service.impl;

import com.quiroz.prestamoservice.dto.AuthResponse;
import com.quiroz.prestamoservice.dto.LoginRequest;
import com.quiroz.prestamoservice.dto.RegisterRequest;
import com.quiroz.prestamoservice.entity.Usuario;
import com.quiroz.prestamoservice.exception.BusinessException;
import com.quiroz.prestamoservice.repository.UsuarioRepository;
import com.quiroz.prestamoservice.security.JwtService;
import com.quiroz.prestamoservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException("El correo electrónico ya se encuentra registrado", HttpStatus.CONFLICT);
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("USER")
                .build();

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.getPassword()
                )
        );

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        String token = jwtService.generateToken(usuario.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .build();
    }
}
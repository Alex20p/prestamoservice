package com.quiroz.prestamoservice.controller;

import com.quiroz.prestamoservice.dto.PrestamoRequest;
import com.quiroz.prestamoservice.dto.PrestamoResponse;
import com.quiroz.prestamoservice.dto.ResumenCarteraResponse;
import com.quiroz.prestamoservice.service.PrestamoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @GetMapping
    public ResponseEntity<List<PrestamoResponse>> listarPrestamos(
            Authentication authentication,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean pagado,
            @RequestParam(required = false) Boolean vencido) {
        String email = authentication.getName();
        List<PrestamoResponse> prestamos = prestamoService.listarPrestamos(email, buscar, pagado, vencido);
        return ResponseEntity.ok(prestamos);
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenCarteraResponse> obtenerResumen(Authentication authentication) {
        String email = authentication.getName();
        ResumenCarteraResponse resumen = prestamoService.obtenerResumen(email);
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoResponse> obtenerPorId(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        PrestamoResponse prestamo = prestamoService.obtenerPorId(id, email);
        return ResponseEntity.ok(prestamo);
    }

    @PostMapping
    public ResponseEntity<PrestamoResponse> crearPrestamo(
            @Valid @RequestBody PrestamoRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        PrestamoResponse creado = prestamoService.crearPrestamo(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrestamoResponse> actualizarPrestamo(
            @PathVariable Long id,
            @Valid @RequestBody PrestamoRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        PrestamoResponse actualizado = prestamoService.actualizarPrestamo(id, request, email);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPrestamo(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        prestamoService.eliminarPrestamo(id, email);
        return ResponseEntity.noContent().build();
    }
}
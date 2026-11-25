package com.quiroz.prestamoservice.service;

import com.quiroz.prestamoservice.dto.PrestamoRequest;
import com.quiroz.prestamoservice.dto.PrestamoResponse;
import com.quiroz.prestamoservice.dto.ResumenCarteraResponse;

import java.util.List;

public interface PrestamoService {
    List<PrestamoResponse> listarPrestamos(String email, String buscar, Boolean pagado, Boolean vencido);
    PrestamoResponse obtenerPorId(Long id, String email);
    PrestamoResponse crearPrestamo(PrestamoRequest request, String email);
    PrestamoResponse actualizarPrestamo(Long id, PrestamoRequest request, String email);
    void eliminarPrestamo(Long id, String email);
    ResumenCarteraResponse obtenerResumen(String email);
}
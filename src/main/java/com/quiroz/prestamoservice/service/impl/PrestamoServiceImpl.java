package com.quiroz.prestamoservice.service.impl;

import com.quiroz.prestamoservice.dto.PrestamoRequest;
import com.quiroz.prestamoservice.dto.PrestamoResponse;
import com.quiroz.prestamoservice.dto.ResumenCarteraResponse;
import com.quiroz.prestamoservice.entity.Prestamo;
import com.quiroz.prestamoservice.entity.Usuario;
import com.quiroz.prestamoservice.exception.BusinessException;
import com.quiroz.prestamoservice.exception.ResourceNotFoundException;
import com.quiroz.prestamoservice.repository.PrestamoRepository;
import com.quiroz.prestamoservice.repository.UsuarioRepository;
import com.quiroz.prestamoservice.service.PrestamoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrestamoServiceImpl implements PrestamoService {

    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;

    public PrestamoServiceImpl(PrestamoRepository prestamoRepository, UsuarioRepository usuarioRepository) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    private LocalDate getFechaActualPeru() {
        return LocalDate.now(ZONA_PERU);
    }

    private Usuario getUsuarioByEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario prestamista no encontrado"));
    }

    public static BigDecimal calcularTotalCobrar(BigDecimal monto, BigDecimal interes) {
        if (monto == null || interes == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal ganancia = monto.multiply(interes).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return monto.add(ganancia).setScale(2, RoundingMode.HALF_UP);
    }

    private PrestamoResponse mapToResponse(Prestamo prestamo) {
        BigDecimal totalCobrar = calcularTotalCobrar(prestamo.getMonto(), prestamo.getInteres());
        return PrestamoResponse.builder()
                .id(prestamo.getId())
                .cliente(prestamo.getCliente())
                .dni(prestamo.getDni())
                .monto(prestamo.getMonto().setScale(2, RoundingMode.HALF_UP))
                .interes(prestamo.getInteres().setScale(2, RoundingMode.HALF_UP))
                .totalCobrar(totalCobrar)
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaVencimiento(prestamo.getFechaVencimiento())
                .pagado(prestamo.getPagado())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoResponse> listarPrestamos(String email, String buscar, Boolean pagado, Boolean vencido) {
        Usuario usuario = getUsuarioByEmail(email);
        LocalDate fechaActual = getFechaActualPeru();

        String buscarParam = (buscar != null && !buscar.trim().isEmpty()) ? buscar.trim() : null;

        List<Prestamo> prestamos = prestamoRepository.findWithFilters(usuario, buscarParam, pagado, vencido, fechaActual);

        return prestamos.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PrestamoResponse obtenerPorId(Long id, String email) {
        Usuario usuario = getUsuarioByEmail(email);
        Prestamo prestamo = prestamoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + id));
        return mapToResponse(prestamo);
    }

    @Override
    @Transactional
    public PrestamoResponse crearPrestamo(PrestamoRequest request, String email) {
        Usuario usuario = getUsuarioByEmail(email);
        LocalDate fechaPrestamo = getFechaActualPeru();

        if (request.getFechaVencimiento().isBefore(fechaPrestamo)) {
            throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha del préstamo (" + fechaPrestamo + ")");
        }

        Prestamo prestamo = Prestamo.builder()
                .cliente(request.getCliente().trim())
                .dni(request.getDni().trim())
                .monto(request.getMonto().setScale(2, RoundingMode.HALF_UP))
                .interes(request.getInteres().setScale(2, RoundingMode.HALF_UP))
                .fechaPrestamo(fechaPrestamo)
                .fechaVencimiento(request.getFechaVencimiento())
                .pagado(false)
                .usuario(usuario)
                .build();

        Prestamo guardado = prestamoRepository.save(prestamo);
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public PrestamoResponse actualizarPrestamo(Long id, PrestamoRequest request, String email) {
        Usuario usuario = getUsuarioByEmail(email);
        Prestamo prestamo = prestamoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + id));

        if (request.getFechaVencimiento().isBefore(prestamo.getFechaPrestamo())) {
            throw new BusinessException("La fecha de vencimiento no puede ser anterior a la fecha original del préstamo (" + prestamo.getFechaPrestamo() + ")");
        }

        prestamo.setCliente(request.getCliente().trim());
        prestamo.setDni(request.getDni().trim());
        prestamo.setMonto(request.getMonto().setScale(2, RoundingMode.HALF_UP));
        prestamo.setInteres(request.getInteres().setScale(2, RoundingMode.HALF_UP));
        prestamo.setFechaVencimiento(request.getFechaVencimiento());

        if (request.getPagado() != null) {
            prestamo.setPagado(request.getPagado());
        }

        Prestamo actualizado = prestamoRepository.save(prestamo);
        return mapToResponse(actualizado);
    }

    @Override
    @Transactional
    public void eliminarPrestamo(Long id, String email) {
        Usuario usuario = getUsuarioByEmail(email);
        Prestamo prestamo = prestamoRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + id));
        prestamoRepository.delete(prestamo);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenCarteraResponse obtenerResumen(String email) {
        Usuario usuario = getUsuarioByEmail(email);
        List<Prestamo> prestamos = prestamoRepository.findByUsuarioOrderByFechaPrestamoDescIdDesc(usuario);

        LocalDate fechaActual = getFechaActualPeru();

        BigDecimal capitalPrestado = BigDecimal.ZERO;
        BigDecimal totalPorCobrar = BigDecimal.ZERO;
        BigDecimal gananciaEsperada = BigDecimal.ZERO;

        long cantidadPrestamos = prestamos.size();
        long cantidadPendientes = 0;
        long cantidadPagados = 0;
        long cantidadVencidos = 0;

        for (Prestamo p : prestamos) {
            BigDecimal monto = p.getMonto();
            BigDecimal ganancia = monto.multiply(p.getInteres()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal totalCobrar = monto.add(ganancia);

            capitalPrestado = capitalPrestado.add(monto);
            gananciaEsperada = gananciaEsperada.add(ganancia);

            if (Boolean.TRUE.equals(p.getPagado())) {
                cantidadPagados++;
            } else {
                cantidadPendientes++;
                totalPorCobrar = totalPorCobrar.add(totalCobrar);
                if (p.getFechaVencimiento().isBefore(fechaActual)) {
                    cantidadVencidos++;
                }
            }
        }

        return ResumenCarteraResponse.builder()
                .capitalPrestado(capitalPrestado.setScale(2, RoundingMode.HALF_UP))
                .totalPorCobrar(totalPorCobrar.setScale(2, RoundingMode.HALF_UP))
                .gananciaEsperada(gananciaEsperada.setScale(2, RoundingMode.HALF_UP))
                .cantidadPrestamos(cantidadPrestamos)
                .cantidadPendientes(cantidadPendientes)
                .cantidadPagados(cantidadPagados)
                .cantidadVencidos(cantidadVencidos)
                .build();
    }
}
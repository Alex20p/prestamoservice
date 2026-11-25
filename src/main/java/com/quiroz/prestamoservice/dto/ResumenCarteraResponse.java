package com.quiroz.prestamoservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenCarteraResponse {
    private BigDecimal capitalPrestado;
    private BigDecimal totalPorCobrar;
    private BigDecimal gananciaEsperada;
    private Long cantidadPrestamos;
    private Long cantidadPendientes;
    private Long cantidadPagados;
    private Long cantidadVencidos;
}
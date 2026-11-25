package com.quiroz.prestamoservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestamoResponse {
    private Long id;
    private String cliente;
    private String dni;
    private BigDecimal monto;
    private BigDecimal interes;
    private BigDecimal totalCobrar;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaPrestamo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaVencimiento;

    private Boolean pagado;
}
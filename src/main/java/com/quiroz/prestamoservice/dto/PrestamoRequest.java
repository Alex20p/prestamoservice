package com.quiroz.prestamoservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestamoRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre del cliente no puede exceder 100 caracteres")
    private String cliente;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^\\d{8}$", message = "El DNI debe contener exactamente ocho dígitos numéricos")
    private String dni;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2, message = "El monto debe tener máximo 8 enteros y 2 decimales")
    private BigDecimal monto;

    @NotNull(message = "El porcentaje de interés es obligatorio")
    @DecimalMin(value = "0.00", message = "El interés debe ser mayor o igual a cero")
    @Digits(integer = 3, fraction = 2, message = "El interés debe tener máximo 3 enteros y 2 decimales")
    private BigDecimal interes;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;

    private Boolean pagado;
}
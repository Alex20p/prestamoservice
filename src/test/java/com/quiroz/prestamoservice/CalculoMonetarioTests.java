package com.quiroz.prestamoservice;

import com.quiroz.prestamoservice.service.impl.PrestamoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculoMonetarioTests {

    @Test
    @DisplayName("Calcular total a cobrar con monto 1000 e interes 10% da 1100.00")
    void testCalculoEstandar() {
        BigDecimal monto = new BigDecimal("1000.00");
        BigDecimal interes = new BigDecimal("10.00");

        BigDecimal totalCobrar = PrestamoServiceImpl.calcularTotalCobrar(monto, interes);

        assertEquals(new BigDecimal("1100.00"), totalCobrar);
    }

    @Test
    @DisplayName("Calcular total a cobrar con interes cero")
    void testCalculoInteresCero() {
        BigDecimal monto = new BigDecimal("500.00");
        BigDecimal interes = new BigDecimal("0.00");

        BigDecimal totalCobrar = PrestamoServiceImpl.calcularTotalCobrar(monto, interes);

        assertEquals(new BigDecimal("500.00"), totalCobrar);
    }

    @Test
    @DisplayName("Calcular total con decimales y redondeo HALF_UP")
    void testCalculoConDecimalesYRedondeo() {
        BigDecimal monto = new BigDecimal("250.75");
        BigDecimal interes = new BigDecimal("15.50");
        BigDecimal totalCobrar = PrestamoServiceImpl.calcularTotalCobrar(monto, interes);

        assertEquals(new BigDecimal("289.62"), totalCobrar);
    }

    @Test
    @DisplayName("Manejo seguro de valores nulos")
    void testValoresNulos() {
        BigDecimal total1 = PrestamoServiceImpl.calcularTotalCobrar(null, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("0.00"), total1);

        BigDecimal total2 = PrestamoServiceImpl.calcularTotalCobrar(new BigDecimal("100.00"), null);
        assertEquals(new BigDecimal("0.00"), total2);
    }
}
package com.quiroz.prestamoservice.repository;

import com.quiroz.prestamoservice.entity.Prestamo;
import com.quiroz.prestamoservice.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioOrderByFechaPrestamoDescIdDesc(Usuario usuario);

    Optional<Prestamo> findByIdAndUsuario(Long id, Usuario usuario);

    @Query("SELECT p FROM Prestamo p WHERE p.usuario = :usuario " +
           "AND (:buscar IS NULL OR :buscar = '' OR LOWER(p.cliente) LIKE LOWER(CONCAT('%', :buscar, '%')) OR p.dni LIKE CONCAT('%', :buscar, '%')) " +
           "AND (:pagado IS NULL OR p.pagado = :pagado) " +
           "AND (:vencido IS NULL OR (:vencido = true AND p.pagado = false AND p.fechaVencimiento < :fechaActual) OR (:vencido = false AND (p.pagado = true OR p.fechaVencimiento >= :fechaActual))) " +
           "ORDER BY p.fechaPrestamo DESC, p.id DESC")
    List<Prestamo> findWithFilters(
            @Param("usuario") Usuario usuario,
            @Param("buscar") String buscar,
            @Param("pagado") Boolean pagado,
            @Param("vencido") Boolean vencido,
            @Param("fechaActual") LocalDate fechaActual
    );
}
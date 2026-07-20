package org.reciclame.demo.repository;

import org.reciclame.demo.model.MinioPendingDeletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MinioPendingDeletionRepository extends JpaRepository<MinioPendingDeletion, Long> {

    /**
     * Busca una entrada pendiente para el mismo objectName. Útil para evitar duplicados:
     * si ya hay una purga pendiente para el mismo archivo, no creamos otra.
     */
    Optional<MinioPendingDeletion> findFirstByObjectNameAndEstado(String objectName, String estado);

    /**
     * Todas las entradas cuya fechaProgramada ya venció y siguen en PENDIENTE.
     * Lo que el scheduler debe procesar.
     */
    @Query("SELECT p FROM MinioPendingDeletion p " +
           "WHERE p.estado = 'PENDIENTE' AND p.fechaProgramada <= :ahora " +
           "ORDER BY p.fechaProgramada ASC")
    List<MinioPendingDeletion> findVencidos(@Param("ahora") Instant ahora);

    /**
     * Cuenta cuántas purgas pendientes hay actualmente, para dashboards / health checks.
     */
    long countByEstado(String estado);
}

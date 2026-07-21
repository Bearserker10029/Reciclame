package org.reciclame.demo.repository;

import org.reciclame.demo.model.SolicitudesRespaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BackupRepository extends JpaRepository<SolicitudesRespaldo, Long> {

    List<SolicitudesRespaldo> findByEstadoActivoTrueOrderByCreatedAtDesc();

    List<SolicitudesRespaldo> findByEstadoAndEstadoActivoTrueOrderByCreatedAtDesc(String estado);

    /**
     * Busca el backup de emergencia más reciente (storageKey fijo) y activo.
     * Se usa en {@code BackupService.eliminarBackupEmergenciaAnterior} antes de
     * crear un nuevo backup de emergencia (solo 1 archivo a la vez).
     */
    SolicitudesRespaldo findFirstByStorageKeyAndEstadoActivoOrderByIdDesc(String storageKey, Boolean estadoActivo);

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.estado = :estado AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findByEstado(@Param("estado") String estado);

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.esBackupPreRestauracion = false AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findBackupsPrincipales();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.esBackupPreRestauracion = true AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findBackupsPreRestauracion();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.restauradoPor IS NOT NULL AND b.estadoActivo = true ORDER BY b.fechaRestauracion DESC")
    List<SolicitudesRespaldo> findRestaurados();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.estado = 'COMPLETADO' AND b.estadoActivo = true ORDER BY b.fechaCompletado DESC")
    List<SolicitudesRespaldo> findCompletados();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.estado = 'FALLIDO' AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findFallidos();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.estado = 'EN_PROCESO' AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findEnProceso();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.estado = 'COMPLETADO' AND b.estadoActivo = true ORDER BY b.fechaCompletado DESC LIMIT 1")
    SolicitudesRespaldo findUltimoCompletado();

    @Query("SELECT COUNT(b) FROM SolicitudesRespaldo b WHERE b.estadoActivo = true")
    long countTotal();

    @Query("SELECT COUNT(b) FROM SolicitudesRespaldo b WHERE b.estado = 'COMPLETADO' AND b.estadoActivo = true")
    long countCompletados();

    @Query("SELECT COUNT(b) FROM SolicitudesRespaldo b WHERE b.estado = 'FALLIDO' AND b.estadoActivo = true")
    long countFallidos();

    @Query("SELECT COUNT(b) FROM SolicitudesRespaldo b WHERE b.estado = 'EN_PROCESO' AND b.estadoActivo = true")
    long countEnProceso();

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.tipo = :tipo AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findByTipo(@Param("tipo") String tipo);

    @Query("SELECT b FROM SolicitudesRespaldo b WHERE b.createdAt >= :desde AND b.createdAt <= :hasta AND b.estadoActivo = true ORDER BY b.createdAt DESC")
    List<SolicitudesRespaldo> findByFechaRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
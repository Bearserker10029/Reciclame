package org.reciclame.demo.repository;

import org.reciclame.demo.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByUsuarioId(Long usuarioId);

    List<Auditoria> findByAccion(String accion);

    List<Auditoria> findByModulo(String modulo);

    @Query("SELECT a FROM Auditoria a WHERE a.fechaAccion >= :desde AND a.fechaAccion <= :hasta AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    List<Auditoria> findByFechaRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("SELECT a FROM Auditoria a WHERE a.usuarioId = :usuarioId AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByUsuarioIdPaginado(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findAllPaginado(Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.modulo = :modulo AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByModuloPaginado(@Param("modulo") String modulo, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.accion = :accion AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByAccionPaginado(@Param("accion") String accion, Pageable pageable);
}

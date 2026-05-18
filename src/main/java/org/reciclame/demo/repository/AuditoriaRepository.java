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

    @Query("SELECT a FROM Auditoria a WHERE a.fechaAccion >= :desde AND a.fechaAccion <= :hasta " +
           "AND a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION') " +
           "ORDER BY a.fechaAccion DESC")
    List<Auditoria> findByFechaRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("SELECT a FROM Auditoria a WHERE a.usuarioId = :usuarioId AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")    
    Page<Auditoria> findByUsuarioIdPaginado(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION') " +
           "ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findAllPaginado(Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.estadoActivo = true " +
           "AND a.modulo != 'SEGURIDAD' " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION', 'ELIMINAR_USUARIO') " +
           "ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findAllExcludingSeguridad(Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.modulo = :modulo AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByModuloPaginado(@Param("modulo") String modulo, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.usuarioId = :usuarioId " +
           "AND a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION', 'ELIMINAR_USUARIO') " +
           "ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByUsuarioIdExcludingImpersonacion(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.accion = :accion AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findByAccionPaginado(@Param("accion") String accion, Pageable pageable);

    @Query("SELECT a FROM Auditoria a WHERE a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION') " +
           "ORDER BY a.fechaAccion DESC LIMIT 100")
    List<Auditoria> findTop100ByOrderByFechaAccionDesc();

    @Query("SELECT a FROM Auditoria a WHERE (a.modulo = 'SEGURIDAD' OR a.estado = 'FALLIDO' " +
           "OR a.accion IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION', 'BLOQUEAR_USUARIO', " +
           "'ELIMINAR_USUARIO', 'CAMBIAR_PASSWORD', 'LOGIN', 'LOGOUT')) " +
           "AND a.estadoActivo = true ORDER BY a.fechaAccion DESC")
    Page<Auditoria> findEventosSeguridad(Pageable pageable);

    @Query("SELECT a.ipAddress, COUNT(a) FROM Auditoria a " +
           "WHERE a.estado = 'FALLIDO' AND a.ipAddress IS NOT NULL " +
           "AND a.estadoActivo = true " +
           "GROUP BY a.ipAddress ORDER BY COUNT(a) DESC")
    List<Object[]> topIPsFallidas(Pageable pageable);

    @Query("SELECT a.usuarioId, a.usuarioNombre, COUNT(a) FROM Auditoria a " +
           "WHERE a.estado = 'FALLIDO' AND a.usuarioId IS NOT NULL " +
           "AND a.estadoActivo = true " +
           "GROUP BY a.usuarioId, a.usuarioNombre ORDER BY COUNT(a) DESC")
    List<Object[]> topUsuariosFallidos(Pageable pageable);

    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.fechaAccion >= :desde AND a.fechaAccion < :hasta AND a.estadoActivo = true")
    long contarEntreFechas(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.estado = 'FALLIDO' " +
           "AND a.fechaAccion >= :desde AND a.estadoActivo = true")
    long contarFallidosDesde(@Param("desde") Instant desde);

    @Query("SELECT COUNT(a) FROM Auditoria a WHERE a.accion = 'IMPERSONAR_USUARIO' " +
           "AND a.estadoActivo = true")
    long contarImpersonaciones();

    @Query("SELECT a.modulo, COUNT(a) FROM Auditoria a " +
           "WHERE a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION') " +
           "GROUP BY a.modulo ORDER BY COUNT(a) DESC")
    List<Object[]> conteoPorModulo();

    @Query("SELECT a.accion, COUNT(a) FROM Auditoria a " +
           "WHERE a.estadoActivo = true " +
           "AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION') " +
           "GROUP BY a.accion ORDER BY COUNT(a) DESC")
    List<Object[]> conteoPorAccion();

    @Query("SELECT DISTINCT a.modulo FROM Auditoria a WHERE a.estadoActivo = true")
    List<String> modulosDistinct();
}

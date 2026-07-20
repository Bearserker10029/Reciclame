package org.reciclame.demo.repository;

import org.reciclame.demo.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    /**
     * Trae el último registro de auditoría ordenado por id descendente,
     * aplicando un lock pesimista a nivel de fila (SELECT ... FOR UPDATE).
     * Se usa para serializar el cálculo del hash chain: dos inserciones
     * simultáneas deben esperar a que la primera obtenga el
     * {@code hashActual} del último bloque antes de calcular el suyo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auditoria a ORDER BY a.id DESC")
    List<Auditoria> findTopByOrderByIdDescWithLock(Pageable pageable);

    /**
     * Igual al anterior pero sin lock — útil para verificación de
     * integridad y seed inicial.
     */
    @Query("SELECT a FROM Auditoria a ORDER BY a.id DESC")
    List<Auditoria> findTopByOrderByIdDesc(Pageable pageable);

    /**
     * Primer registro histórico (id mínimo) — punto de partida de la
     * cadena. Si la tabla está vacía devuelve {@link Optional#empty()}.
     */
    @Query("SELECT a FROM Auditoria a ORDER BY a.id ASC")
    List<Auditoria> findFirstByOrderByIdAsc(Pageable pageable);

    /**
     * Devuelve todas las filas ordenadas por id ascendente (caden completo).
     * Para verificación de integridad — evita paginar y perder el orden.
     */
    @Query("SELECT a FROM Auditoria a ORDER BY a.id ASC")
    List<Auditoria> findAllOrderedById();

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

    /**
     * Lista auditorías (no impersonación ni impersonación-salida) excluyendo
     * además las acciones ejecutadas por usuarios cuyo {@code usuarioId}
     * pertenece a un usuario con rol SUPERADMIN.
     *
     <p>Se usa en el contexto del rol ADMIN para que su reporte de auditoría
     * nunca muestre lo que hizo un superadmin, sin depender de carga LAZY de
     * la entidad {@code Usuario} ni de lookups en memoria.</p>
     */
    @Query("""
            SELECT a FROM Auditoria a
            WHERE a.estadoActivo = true
              AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION')
              AND (
                    a.usuarioId IS NULL
                    OR NOT EXISTS (
                          SELECT 1 FROM Usuario u
                          WHERE u.id = a.usuarioId
                            AND u.rol IS NOT NULL
                            AND UPPER(u.rol.nombre) = 'SUPERADMIN'
                    )
              )
            ORDER BY a.fechaAccion DESC
            """)
    Page<Auditoria> findAllExcluyendoAccionesDeSuperadmin(Pageable pageable);

    /**
     * Trazabilidad para ADMIN: muestra todo lo que ve superadmin excepto
     *  - módulo SEGURIDAD
     *  - acciones IMPERSONAR_USUARIO, SALIR_IMPERSONACION, ELIMINAR_USUARIO
     *  - acciones ejecutadas por usuarios con rol SUPERADMIN
     */
    @Query("""
            SELECT a FROM Auditoria a
            WHERE a.estadoActivo = true
              AND a.modulo != 'SEGURIDAD'
              AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION', 'ELIMINAR_USUARIO')
              AND (
                    a.usuarioId IS NULL
                    OR NOT EXISTS (
                          SELECT 1 FROM Usuario u
                          WHERE u.id = a.usuarioId
                            AND u.rol IS NOT NULL
                            AND UPPER(u.rol.nombre) = 'SUPERADMIN'
                    )
              )
            ORDER BY a.fechaAccion DESC
            """)
    Page<Auditoria> findAllExcludingSeguridadYAccionesDeSuperadmin(Pageable pageable);

    /**
     * Variante limitada para los exports PDF/XLSX/CSV.
     */
    @Query(value = """
            SELECT a FROM Auditoria a
            WHERE a.estadoActivo = true
              AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION')
              AND (
                    a.usuarioId IS NULL
                    OR NOT EXISTS (
                          SELECT 1 FROM Usuario u
                          WHERE u.id = a.usuarioId
                            AND u.rol IS NOT NULL
                            AND UPPER(u.rol.nombre) = 'SUPERADMIN'
                    )
              )
            ORDER BY a.fechaAccion DESC
            """,
           countQuery = """
            SELECT COUNT(a) FROM Auditoria a
            WHERE a.estadoActivo = true
              AND a.accion NOT IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION')
              AND (
                    a.usuarioId IS NULL
                    OR NOT EXISTS (
                          SELECT 1 FROM Usuario u
                          WHERE u.id = a.usuarioId
                            AND u.rol IS NOT NULL
                            AND UPPER(u.rol.nombre) = 'SUPERADMIN'
                    )
              )
            """)
    Page<Auditoria> findAllExcluyendoAccionesDeSuperadminP(Pageable pageable);

    /**
     * Eventos relevantes para el panel de Auditoría de Seguridad del SuperAdmin.
     *
     * <p>Reglas:</p>
     * <ul>
     *   <li>Todo lo del módulo {@code SEGURIDAD} (cambio de password, bloqueos
     *       automáticos, etc.) — siempre es un evento de seguridad.</li>
     *   <li>Todo evento con {@code estado='FALLIDO'} — los intentos de login
     *       fallidos que se registran desde {@code AuthFailureHandler} entran
     *       por aquí.</li>
     *   <li>Acciones críticas: {@code IMPERSONAR_USUARIO},
     *       {@code SALIR_IMPERSONACION}, {@code BLOQUEAR_USUARIO},
     *       {@code ELIMINAR_USUARIO}, {@code CAMBIAR_PASSWORD}.</li>
     * </ul>
     *
     * <p><b>Nota:</b> {@code LOGIN} y {@code LOGOUT} NO se incluyen aquí a
     * propósito. Esos eventos pertenecen a la pantalla de Trazabilidad (el
     * módulo del registro es {@code AUTENTICACION}, no {@code SEGURIDAD}),
     * no a la de Auditoría. Antes aparecían en Auditoría por error y se
     * quitaron del IN clause para respetar la separación trazabilidad vs
     * seguridad. Si en el futuro hace falta verlos también desde auditoría,
     * moverlos a una pestaña aparte en lugar de mezclarlos aquí.</p>
     */
    @Query("SELECT a FROM Auditoria a WHERE (a.modulo = 'SEGURIDAD' OR a.estado = 'FALLIDO' " +
           "OR a.accion IN ('IMPERSONAR_USUARIO', 'SALIR_IMPERSONACION', 'BLOQUEAR_USUARIO', " +
           "'ELIMINAR_USUARIO', 'CAMBIAR_PASSWORD')) " +
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

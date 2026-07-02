package org.reciclame.demo.repository;

import org.reciclame.demo.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoCorporativo(String correoCorporativo);

    Optional<Usuario> findByDni(String dni);

    boolean existsByCorreoCorporativo(String correoCorporativo);

    boolean existsByDni(String dni);

    boolean existsByCorreoCorporativoAndEstadoActivoTrue(String correoCorporativo);

    boolean existsByDniAndEstadoActivoTrue(String dni);

    List<Usuario> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt >= :desde AND u.estadoActivo = true")
    long contarNuevosDesde(@Param("desde") Instant desde);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt >= :desde AND u.createdAt < :hasta AND u.estadoActivo = true")
    long contarEntreFechas(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    List<Usuario> findByRolNombre(String nombre);

    @Query("""
            SELECT u FROM Usuario u
            WHERE u.empresa.id = :empresaId
              AND u.id <> :excludeId
              AND u.estadoActivo = true
              AND u.estadoCuenta = 'ACTIVO'
            ORDER BY u.nombreCompleto ASC
            """)
    List<Usuario> findCompanerosEmpresa(@Param("empresaId") Long empresaId,
                                        @Param("excludeId") Long excludeId);

    /**
     * Incrementa el contador de intentos fallidos en 1 sin tocar el resto
     * del usuario. Usado por el AuthenticationFailureHandler para
     * contabilizar cada login fallido y comparar contra
     * parametros_sistema.max_intentos_login.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = COALESCE(u.intentosFallidos, 0) + 1 WHERE u.id = :id")
    int incrementarIntentosFallidos(@Param("id") Long id);

    /**
     * Marca la cuenta como bloqueada y deja intentosFallidos en 0
     * para que el siguiente flujo de "desbloqueo por Admin" arranque limpio.
     * Registra fechaBloqueo para permitir el auto-desbloqueo temporal
     * (parametros_sistema.tiempo_bloqueo_minutos).
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.cuentaBloqueada = true, u.estadoCuenta = 'BLOQUEADO', u.intentosFallidos = 0, u.fechaBloqueo = CURRENT_TIMESTAMP WHERE u.id = :id")
    int bloquearCuenta(@Param("id") Long id);

    /**
     * Resetea intentosFallidos y desbloquea la cuenta. Se ejecuta al
     * detectar un login exitoso de un usuario que previamente tuvo
     * intentos fallidos o estaba bloqueado, o al vencer el tiempo de
     * bloqueo automático.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.intentosFallidos = 0, u.cuentaBloqueada = false, u.estadoCuenta = 'ACTIVO', u.fechaBloqueo = null WHERE u.id = :id")
    int resetearIntentosYDesbloquear(@Param("id") Long id);

    /**
     * Búsqueda global de usuarios activos.
     * Filtra por roles permitidos (excluye SUPERADMIN por defecto).
     * Usado por ADMINISTRADOR (ve SOCIO + VISUALIZADOR + ADMINISTRADOR).
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.estadoActivo = true
              AND u.rol IS NOT NULL
              AND u.rol.nombre IN :rolesPermitidos
              AND (LOWER(u.nombreCompleto)    LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.correoCorporativo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.dni)              LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.cargo, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.nombreCompleto ASC
            """)
    List<Usuario> buscarPorRoles(@Param("q") String q,
                                 @Param("rolesPermitidos") List<String> rolesPermitidos,
                                 Pageable pageable);

    /**
     * Búsqueda global de usuarios SIN filtro de rol (incluye SUPERADMIN).
     * Usado por SUPERADMIN.
     */
    @Query("""
            SELECT u FROM Usuario u
            WHERE u.estadoActivo = true
              AND (LOWER(u.nombreCompleto)    LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.correoCorporativo) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.dni)              LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.cargo, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.nombreCompleto ASC
            """)
    List<Usuario> buscarTodos(@Param("q") String q, Pageable pageable);
}

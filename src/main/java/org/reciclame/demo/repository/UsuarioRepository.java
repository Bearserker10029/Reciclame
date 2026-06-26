package org.reciclame.demo.repository;

import org.reciclame.demo.model.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

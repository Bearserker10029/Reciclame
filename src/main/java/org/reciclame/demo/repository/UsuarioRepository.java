package org.reciclame.demo.repository;

import org.reciclame.demo.model.Usuario;
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

    List<Usuario> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt >= :desde AND u.estadoActivo = true")
    long contarNuevosDesde(@Param("desde") Instant desde);

    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.createdAt >= :desde AND u.createdAt < :hasta AND u.estadoActivo = true")
    long contarEntreFechas(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}

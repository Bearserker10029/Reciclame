package org.reciclame.demo.repository;

import org.reciclame.demo.model.PreferenciasNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreferenciasNotificacionRepository extends JpaRepository<PreferenciasNotificacion, Long> {
    Optional<PreferenciasNotificacion> findByUsuarioId(Long usuarioId);
}

package org.reciclame.demo.repository;

import org.reciclame.demo.model.Notificacione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacione, Long> {

    List<Notificacione> findByUsuarioIdAndEstadoActivoTrueOrderByCreatedAtDesc(Long usuarioId);

    long countByUsuarioIdAndLeidaFalseAndEstadoActivoTrue(Long usuarioId);
}

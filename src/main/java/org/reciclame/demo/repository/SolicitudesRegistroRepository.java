package org.reciclame.demo.repository;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.SolicitudesRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudesRegistroRepository extends JpaRepository<SolicitudesRegistro, Long> {
    boolean existsByCorreoCorporativo(String correoCorporativo);
    boolean existsByCorreoCorporativoAndEstado(String correoCorporativo, EstadoSolicitud estado);
    boolean existsByDni(String dni);

    boolean existsByDniAndEstadoIn(String dni, java.util.List<EstadoSolicitud> estados);
    Optional<SolicitudesRegistro> findByCorreoCorporativo(String correoCorporativo);
    List<SolicitudesRegistro> findByEstadoOrderByCreatedAtDesc(EstadoSolicitud estado);
    List<SolicitudesRegistro> findByEstadoNotOrderByCreatedAtDesc(EstadoSolicitud estado);
}

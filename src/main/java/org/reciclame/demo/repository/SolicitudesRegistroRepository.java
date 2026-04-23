package org.reciclame.demo.repository;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.SolicitudesRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolicitudesRegistroRepository extends JpaRepository<SolicitudesRegistro, Long> {
    boolean existsByCorreoCorporativo(String correoCorporativo);
    boolean existsByCorreoCorporativoAndEstado(String correoCorporativo, EstadoSolicitud estado);
    boolean existsByDni(String dni);
    Optional<SolicitudesRegistro> findByCorreoCorporativo(String correoCorporativo);
}

package org.reciclame.demo.repository;

import org.reciclame.demo.model.DocumentosReciente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentosRecienteRepository extends JpaRepository<DocumentosReciente, Long> {

    List<DocumentosReciente> findByUsuario_IdOrderByUltimaVistaDesc(Long usuarioId, Pageable pageable);

    long countByUsuario_Id(Long usuarioId);

    long countByUsuario_IdAndUltimaVistaGreaterThanEqual(Long usuarioId, Instant desde);

    Optional<DocumentosReciente> findByUsuario_IdAndDocumentoTipoAndDocumentoId(
            Long usuarioId, String documentoTipo, Long documentoId);
}

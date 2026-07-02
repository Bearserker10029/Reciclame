package org.reciclame.demo.repository;

import org.reciclame.demo.model.DocumentoCompartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoCompartidoRepository extends JpaRepository<DocumentoCompartido, Long> {

    @Query("SELECT d FROM DocumentoCompartido d " +
           "WHERE d.tipoDocumento = :tipo AND d.documentoId = :documentoId " +
           "ORDER BY d.createdAt ASC")
    List<DocumentoCompartido> findByDocumento(@Param("tipo") String tipo, @Param("documentoId") Long documentoId);

    Optional<DocumentoCompartido> findByTipoDocumentoAndDocumentoIdAndUsuarioId(
            String tipoDocumento, Long documentoId, Long usuarioId);

    void deleteByTipoDocumentoAndDocumentoIdAndUsuarioId(String tipoDocumento, Long documentoId, Long usuarioId);
}

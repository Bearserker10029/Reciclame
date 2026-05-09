package org.reciclame.demo.repository;

import org.reciclame.demo.model.MiUnidadItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiUnidadItemRepository extends JpaRepository<MiUnidadItem, Long> {

    List<MiUnidadItem> findByUsuario_IdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    long countByUsuario_Id(Long usuarioId);

    List<MiUnidadItem> findByUsuario_IdAndCarpetaIsNullOrderByCreatedAtDesc(Long usuarioId);

    List<MiUnidadItem> findByUsuario_IdAndCarpeta_IdOrderByCreatedAtDesc(Long usuarioId, Long carpetaId);

    Optional<MiUnidadItem> findByIdAndUsuario_Id(Long id, Long usuarioId);

    Optional<MiUnidadItem> findFirstByUsuario_IdAndDocumentoTipoAndDocumentoIdOrderByCreatedAtDesc(
            Long usuarioId, String documentoTipo, Long documentoId);

    boolean existsByUsuario_IdAndCarpetaIsNullAndDocumentoTipoAndDocumentoId(
            Long usuarioId, String documentoTipo, Long documentoId);

    boolean existsByUsuario_IdAndCarpeta_IdAndDocumentoTipoAndDocumentoId(
            Long usuarioId, Long carpetaId, String documentoTipo, Long documentoId);

    void deleteByCarpeta_IdAndUsuario_Id(Long carpetaId, Long usuarioId);

    long countByUsuario_IdAndCarpeta_Id(Long usuarioId, Long carpetaId);
}

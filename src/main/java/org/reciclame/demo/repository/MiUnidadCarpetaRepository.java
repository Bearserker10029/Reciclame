package org.reciclame.demo.repository;

import org.reciclame.demo.model.MiUnidadCarpeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiUnidadCarpetaRepository extends JpaRepository<MiUnidadCarpeta, Long> {

    List<MiUnidadCarpeta> findByUsuario_IdAndParentIsNullOrderByNombreAsc(Long usuarioId);

    List<MiUnidadCarpeta> findByUsuario_IdAndParent_IdOrderByNombreAsc(Long usuarioId, Long parentId);

    Optional<MiUnidadCarpeta> findByIdAndUsuario_Id(Long id, Long usuarioId);

    List<MiUnidadCarpeta> findByUsuario_IdOrderByNombreAsc(Long usuarioId);

    long countByUsuario_Id(Long usuarioId);

    long countByUsuario_IdAndParent_Id(Long usuarioId, Long parentId);
}

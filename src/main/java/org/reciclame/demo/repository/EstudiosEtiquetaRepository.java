package org.reciclame.demo.repository;

import org.reciclame.demo.model.EstudiosEtiqueta;
import org.reciclame.demo.model.EstudiosEtiquetaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudiosEtiquetaRepository extends JpaRepository<EstudiosEtiqueta, EstudiosEtiquetaId> {

    @Query("SELECT ee FROM EstudiosEtiqueta ee WHERE ee.estudio.id = :estudioId")
    List<EstudiosEtiqueta> findByEstudioId(@Param("estudioId") Long estudioId);

    @Modifying
    @Query("DELETE FROM EstudiosEtiqueta ee WHERE ee.estudio.id = :estudioId")
    void deleteByEstudioId(@Param("estudioId") Long estudioId);
}

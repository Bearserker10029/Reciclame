package org.reciclame.demo.repository;

import org.reciclame.demo.model.NormativasEtiqueta;
import org.reciclame.demo.model.NormativasEtiquetaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormativasEtiquetaRepository extends JpaRepository<NormativasEtiqueta, NormativasEtiquetaId> {

    @Query("SELECT ne FROM NormativasEtiqueta ne WHERE ne.normativa.id = :normativaId")
    List<NormativasEtiqueta> findByNormativaId(@Param("normativaId") Long normativaId);

    @Modifying
    @Query("DELETE FROM NormativasEtiqueta ne WHERE ne.normativa.id = :normativaId")
    void deleteByNormativaId(@Param("normativaId") Long normativaId);
}

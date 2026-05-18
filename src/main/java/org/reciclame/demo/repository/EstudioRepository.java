package org.reciclame.demo.repository;

import org.reciclame.demo.model.Estudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long> {

    List<Estudio> findByEstadoActivoTrue();

    List<Estudio> findByAnio(Integer anio);

    List<Estudio> findByEsPublicoTrue();

    List<Estudio> findTop100ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(e) FROM Estudio e WHERE e.createdAt >= :desde")
    long contarNuevosDesde(@Param("desde") Instant desde);

    @Query("SELECT COUNT(e) FROM Estudio e WHERE e.createdAt >= :desde AND e.createdAt < :hasta")
    long contarEntreFechas(@Param("desde") Instant desde, @Param("hasta") Instant hasta);
}

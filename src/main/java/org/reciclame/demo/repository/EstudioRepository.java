package org.reciclame.demo.repository;

import org.reciclame.demo.model.Estudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudioRepository extends JpaRepository<Estudio, Long> {

    List<Estudio> findByEstadoActivoTrue();

    List<Estudio> findByAnio(Integer anio);

    List<Estudio> findByEsPublicoTrue();

    List<Estudio> findTop100ByOrderByCreatedAtDesc();
}

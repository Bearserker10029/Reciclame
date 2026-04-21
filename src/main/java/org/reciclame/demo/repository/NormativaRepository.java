package org.reciclame.demo.repository;

import org.reciclame.demo.model.Normativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NormativaRepository extends JpaRepository<Normativa, Long> {

    List<Normativa> findByEstadoActivoTrue();

    List<Normativa> findByAmbito(String ambito);

    List<Normativa> findByClasificacion(String clasificacion);
}

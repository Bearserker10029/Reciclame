package org.reciclame.demo.repository;

import org.reciclame.demo.model.Tematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TematicaRepository extends JpaRepository<Tematica, Long> {

    List<Tematica> findByEstadoActivoTrue();

    Optional<Tematica> findByCodigo(String codigo);
}

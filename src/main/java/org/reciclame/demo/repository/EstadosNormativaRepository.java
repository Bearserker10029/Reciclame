package org.reciclame.demo.repository;

import org.reciclame.demo.model.EstadosNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadosNormativaRepository extends JpaRepository<EstadosNormativa, Long> {

    List<EstadosNormativa> findByEstadoActivoTrueOrderByNombreAsc();

    List<EstadosNormativa> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}

package org.reciclame.demo.repository;

import org.reciclame.demo.model.TiposNormativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TiposNormativaRepository extends JpaRepository<TiposNormativa, Long> {

    List<TiposNormativa> findByEstadoActivoTrueOrderByNombreAsc();

    List<TiposNormativa> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}

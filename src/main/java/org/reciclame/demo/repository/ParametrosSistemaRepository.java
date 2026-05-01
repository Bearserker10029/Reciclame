package org.reciclame.demo.repository;

import org.reciclame.demo.model.ParametrosSistema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametrosSistemaRepository extends JpaRepository<ParametrosSistema, Long> {
    Optional<ParametrosSistema> findByClave(String clave);
    boolean existsByClave(String clave);
}

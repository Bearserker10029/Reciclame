package org.reciclame.demo.repository;

import org.reciclame.demo.model.ConfiguracionUiRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionUiRolRepository extends JpaRepository<ConfiguracionUiRol, Long> {
    Optional<ConfiguracionUiRol> findByRolAndClave(String rol, String clave);
    List<ConfiguracionUiRol> findByRol(String rol);
}

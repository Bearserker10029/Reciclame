package org.reciclame.demo.repository;

import org.reciclame.demo.model.Empresas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresas, Long> {
    boolean existsByRuc(String ruc);
}

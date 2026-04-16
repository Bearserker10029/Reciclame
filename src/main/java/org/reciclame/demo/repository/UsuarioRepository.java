package org.reciclame.demo.repository;

import org.reciclame.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por correo (para login y validar duplicados)
    Optional<Usuario> findByCorreoCorporativo(String correoCorporativo);

    // Buscar por DNI (para validar duplicados al registrar)
    Optional<Usuario> findByDni(String dni);

    // Verificar si ya existe un correo (en validaciones será útil)
    boolean existsByCorreoCorporativo(String correoCorporativo);

    // Verificar si ya existe un DNI
    boolean existsByDni(String dni);
}

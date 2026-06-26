package org.reciclame.demo.repository;

import org.reciclame.demo.model.ChatConversacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversacionRepository extends JpaRepository<ChatConversacion, Long> {

    @Query("""
        SELECT c FROM ChatConversacion c
        WHERE (c.usuarioA.id = :uA AND c.usuarioB.id = :uB)
           OR (c.usuarioA.id = :uB AND c.usuarioB.id = :uA)
        """)
    Optional<ChatConversacion> findEntreUsuarios(@Param("uA") Long uA, @Param("uB") Long uB);

    @Query("""
        SELECT c FROM ChatConversacion c
        WHERE c.usuarioA.id = :usuarioId OR c.usuarioB.id = :usuarioId
        ORDER BY c.updatedAt DESC
        """)
    List<ChatConversacion> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}

package org.reciclame.demo.repository;

import org.reciclame.demo.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    List<ChatMensaje> findByConversacionIdOrderByCreatedAtAsc(Long conversacionId);

    @Query("""
        SELECT COUNT(m) FROM ChatMensaje m
        WHERE m.conversacion.id = :convId
          AND m.remitente.id <> :usuarioId
          AND m.leido = false
        """)
    long contarNoLeidos(@Param("convId") Long convId, @Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("""
        UPDATE ChatMensaje m
        SET m.leido = true
        WHERE m.conversacion.id = :convId
          AND m.remitente.id <> :usuarioId
          AND m.leido = false
        """)
    int marcarComoLeidos(@Param("convId") Long convId, @Param("usuarioId") Long usuarioId);
}

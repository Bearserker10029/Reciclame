package org.reciclame.demo.repository;

import org.reciclame.demo.model.ConversacionesChatbot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ConversacionesChatbotRepository extends JpaRepository<ConversacionesChatbot, Long> {

    Page<ConversacionesChatbot> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ConversacionesChatbot> findByUsuario_IdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    Page<ConversacionesChatbot> findByUsuario_Rol_IdOrderByCreatedAtDesc(Long rolId, Pageable pageable);

    @Query("""
            SELECT c FROM ConversacionesChatbot c
            WHERE (:usuarioId IS NULL OR c.usuario.id = :usuarioId)
              AND (:rolId    IS NULL OR c.usuario.rol.id = :rolId)
              AND (:desde   IS NULL OR c.createdAt >= :desde)
              AND (:hasta   IS NULL OR c.createdAt <= :hasta)
            ORDER BY c.createdAt DESC
            """)
    Page<ConversacionesChatbot> filtrar(@Param("usuarioId") Long usuarioId,
                                        @Param("rolId") Long rolId,
                                        @Param("desde") Instant desde,
                                        @Param("hasta") Instant hasta,
                                        Pageable pageable);

    @Query("""
            SELECT COUNT(c) FROM ConversacionesChatbot c
            WHERE (:desde IS NULL OR c.createdAt >= :desde)
              AND (:hasta IS NULL OR c.createdAt <= :hasta)
            """)
    long countByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(DISTINCT c.usuario.id) FROM ConversacionesChatbot c
            WHERE (:desde IS NULL OR c.createdAt >= :desde)
              AND (:hasta IS NULL OR c.createdAt <= :hasta)
            """)
    long countDistinctUsuariosByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Conteo de conversaciones nuevas por día dentro del rango, ordenado por fecha asc.
     */
    @Query(value = """
            SELECT DATE(c.created_at) AS fecha, COUNT(*) AS total
            FROM conversaciones_chatbot c
            WHERE (:desde IS NULL OR c.created_at >= :desde)
              AND (:hasta IS NULL OR c.created_at <= :hasta)
            GROUP BY DATE(c.created_at)
            ORDER BY DATE(c.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> conversacionesPorDia(@Param("desde") Instant desde,
                                        @Param("hasta") Instant hasta);

    /**
     * Conteo por rol de usuario (para el chart de dona).
     */
    @Query("""
            SELECT r.nombre, COUNT(c)
            FROM ConversacionesChatbot c
              JOIN c.usuario u
              JOIN u.rol r
            WHERE (:desde IS NULL OR c.createdAt >= :desde)
              AND (:hasta IS NULL OR c.createdAt <= :hasta)
            GROUP BY r.nombre
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> conversacionesPorRol(@Param("desde") Instant desde,
                                        @Param("hasta") Instant hasta);

    /**
     * Top usuarios con mas conversaciones en el rango.
     */
    @Query("""
            SELECT u.id, u.nombreCompleto, COUNT(c)
            FROM ConversacionesChatbot c
              JOIN c.usuario u
            WHERE (:desde IS NULL OR c.createdAt >= :desde)
              AND (:hasta IS NULL OR c.createdAt <= :hasta)
            GROUP BY u.id, u.nombreCompleto
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> topUsuarios(@Param("desde") Instant desde,
                                @Param("hasta") Instant hasta,
                                Pageable pageable);
}
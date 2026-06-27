package org.reciclame.demo.repository;

import org.reciclame.demo.model.MensajesChatbot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MensajesChatbotRepository extends JpaRepository<MensajesChatbot, Long> {

    List<MensajesChatbot> findByConversacion_IdOrderByCreatedAtAsc(Long conversacionId);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    long countByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    long countByRangoExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.rol = 'USER'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    long countUserByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.rol = 'USER'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    long countUserByRangoExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.rol = 'BOT'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    long countBotByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.rol = 'BOT'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    long countBotByRangoExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.esError = true
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    long countErroresByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.esError = true
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    long countErroresByRangoExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.derivadoSoporte = true
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    long countDerivadosByRango(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COUNT(m) FROM MensajesChatbot m
            WHERE m.derivadoSoporte = true
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    long countDerivadosByRangoExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Latencia promedio (ms) de los mensajes del bot dentro del rango.
     */
    @Query("""
            SELECT COALESCE(AVG(m.duracionMs), 0)
            FROM MensajesChatbot m
            WHERE m.rol = 'BOT'
              AND m.duracionMs IS NOT NULL
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            """)
    Double avgLatenciaBot(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query("""
            SELECT COALESCE(AVG(m.duracionMs), 0)
            FROM MensajesChatbot m
            WHERE m.rol = 'BOT'
              AND m.duracionMs IS NOT NULL
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            """)
    Double avgLatenciaBotExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Mensajes por día (USER + BOT) — base para el chart diario.
     */
    @Query(value = """
            SELECT DATE(m.created_at) AS fecha, m.rol, COUNT(*) AS total
            FROM mensajes_chatbot m
            WHERE (:desde IS NULL OR m.created_at >= :desde)
              AND (:hasta IS NULL OR m.created_at <= :hasta)
            GROUP BY DATE(m.created_at), m.rol
            ORDER BY DATE(m.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> mensajesPorDia(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query(value = """
            SELECT DATE(m.created_at) AS fecha, m.rol, COUNT(*) AS total
            FROM mensajes_chatbot m
              JOIN conversaciones_chatbot c ON c.id = m.conversacion_id
              JOIN usuario u                ON u.id = c.usuario_id
              LEFT JOIN rol r               ON r.id = u.rol_id
            WHERE (:desde IS NULL OR m.created_at >= :desde)
              AND (:hasta IS NULL OR m.created_at <= :hasta)
              AND UPPER(COALESCE(r.nombre, '')) <> 'SUPERADMIN'
            GROUP BY DATE(m.created_at), m.rol
            ORDER BY DATE(m.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> mensajesPorDiaExcluyendoSuperadmin(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    /**
     * Conteo por intent detectado (Top intents).
     */
    @Query("""
            SELECT COALESCE(m.intentDetectado, 'SIN_INTENT'), COUNT(m)
            FROM MensajesChatbot m
            WHERE m.rol = 'USER'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
            GROUP BY COALESCE(m.intentDetectado, 'SIN_INTENT')
            ORDER BY COUNT(m) DESC
            """)
    List<Object[]> topIntents(@Param("desde") Instant desde,
                              @Param("hasta") Instant hasta,
                              org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT COALESCE(m.intentDetectado, 'SIN_INTENT'), COUNT(m)
            FROM MensajesChatbot m
            WHERE m.rol = 'USER'
              AND (:desde IS NULL OR m.createdAt >= :desde)
              AND (:hasta IS NULL OR m.createdAt <= :hasta)
              AND (
                    m.conversacion IS NULL
                    OR m.conversacion.usuario IS NULL
                    OR m.conversacion.usuario.rol IS NULL
                    OR UPPER(m.conversacion.usuario.rol.nombre) <> 'SUPERADMIN'
              )
            GROUP BY COALESCE(m.intentDetectado, 'SIN_INTENT')
            ORDER BY COUNT(m) DESC
            """)
    List<Object[]> topIntentsExcluyendoSuperadmin(@Param("desde") Instant desde,
                                                  @Param("hasta") Instant hasta,
                                                  org.springframework.data.domain.Pageable pageable);
}
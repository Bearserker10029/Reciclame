package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "conversaciones_chatbot", schema = "reciclame_db", indexes = {
        @Index(name = "idx_conversaciones_usuario",
                columnList = "usuario_id"),
        @Index(name = "idx_conversaciones_fecha",
                columnList = "created_at"),
        @Index(name = "idx_conversaciones_ultimo",
                columnList = "ultimo_mensaje_at")})
public class ConversacionesChatbot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ColumnDefault("'es'")
    @Column(name = "idioma", nullable = false, length = 5)
    private String idioma;

    @Column(name = "titulo", length = 200)
    private String titulo;

    @Lob
    @Column(name = "resumen")
    private String resumen;

    @ColumnDefault("0")
    @Column(name = "total_mensajes", nullable = false)
    private Integer totalMensajes;

    @ColumnDefault("0")
    @Column(name = "duracion_total_ms", nullable = false)
    private Long duracionTotalMs;

    @Column(name = "ultimo_mensaje_at")
    private Instant ultimoMensajeAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @ColumnDefault("0")
    @Column(name = "cerrada_por_usuario", nullable = false)
    private Boolean cerradaPorUsuario;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


}
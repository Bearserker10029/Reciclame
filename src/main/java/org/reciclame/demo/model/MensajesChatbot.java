package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mensajes_chatbot", schema = "reciclame_db", indexes = {
        @Index(name = "idx_mensajes_conversacion",
                columnList = "conversacion_id"),
        @Index(name = "idx_mensajes_fecha",
                columnList = "created_at"),
        @Index(name = "idx_mensajes_rol_fecha",
                columnList = "rol, created_at"),
        @Index(name = "idx_mensajes_error",
                columnList = "es_error, created_at")})
public class MensajesChatbot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private ConversacionesChatbot conversacion;

    @Column(name = "rol", nullable = false, length = 10)
    private String rol;

    @Lob
    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Column(name = "intent_detectado", length = 100)
    private String intentDetectado;

    @Column(name = "confianza", precision = 5, scale = 4)
    private BigDecimal confianza;

    @ColumnDefault("0")
    @Column(name = "derivado_soporte", nullable = false)
    private Boolean derivadoSoporte;

    /** Latencia entre pregunta del usuario y respuesta del bot (ms) */
    @Column(name = "duracion_ms")
    private Long duracionMs;

    /** Tokens aprox. consumidos (entrada+salida) */
    @Column(name = "tokens_estimados")
    private Integer tokensEstimados;

    /** Modelo LLM usado (ej: minimax, gpt-4o-mini) */
    @Column(name = "modelo", length = 60)
    private String modelo;

    /** 1 = hubo error al generar la respuesta */
    @ColumnDefault("0")
    @Column(name = "es_error", nullable = false)
    private Boolean esError;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    /** SQL ejecutado por el agente (truncado a 4000 chars) */
    @Lob
    @Column(name = "sql_ejecutado")
    private String sqlEjecutado;

    /** Cantidad de filas devueltas por la tool SQL */
    @Column(name = "filas_devueltas")
    private Integer filasDevueltas;

    /** Datos extra del callback (intents, scores, etc.) */
    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
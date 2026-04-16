package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "logs_actividad", schema = "reciclame_db", indexes = {
        @Index(name = "idx_logs_reporte_usuario_fecha",
                columnList = "usuario_id, created_at, tipo_accion"),
        @Index(name = "idx_logs_usuario",
                columnList = "usuario_id"),
        @Index(name = "idx_logs_reporte_descarga",
                columnList = "tipo_accion, recurso_tipo, recurso_id, created_at"),
        @Index(name = "idx_logs_tipo_accion",
                columnList = "tipo_accion"),
        @Index(name = "idx_logs_fecha",
                columnList = "created_at")})
public class LogsActividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuarios;

    @Column(name = "tipo_accion", nullable = false, length = 50)
    private String tipoAccion;

    @Column(name = "recurso_tipo", nullable = false, length = 50)
    private String recursoTipo;

    @Column(name = "recurso_id")
    private Long recursoId;

    @Lob
    @Column(name = "descripcion_detalle")
    private String descripcionDetalle;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
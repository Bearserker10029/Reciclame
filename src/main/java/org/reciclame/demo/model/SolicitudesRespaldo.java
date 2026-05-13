package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "solicitudes_respaldo", schema = "reciclame_db", indexes = {
        @Index(name = "idx_respaldos_solicitante",
                columnList = "solicitado_por_id"),
        @Index(name = "idx_respaldos_estado",
                columnList = "estado"),
        @Index(name = "idx_respaldos_fecha",
                columnList = "created_at")})
public class SolicitudesRespaldo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @ColumnDefault("'COMPLETO'")
    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @ColumnDefault("'PENDIENTE'")
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Lob
    @Column(name = "notas")
    private String notas;

    @Column(name = "fecha_completado")
    private Instant fechaCompletado;

    @ColumnDefault("0")
    @Column(name = "es_backup_pre_restauracion", nullable = false)
    private Boolean esBackupPreRestauracion = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurado_por_id")
    private Usuario restauradoPor;

    @Column(name = "fecha_restauracion")
    private Instant fechaRestauracion;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (estadoActivo == null) {
            estadoActivo = true;
        }
        if (esBackupPreRestauracion == null) {
            esBackupPreRestauracion = false;
        }
    }
}
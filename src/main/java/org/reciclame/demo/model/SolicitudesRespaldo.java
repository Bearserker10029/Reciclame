package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "solicitudes_respaldo", schema = "reciclame_db")
public class SolicitudesRespaldo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por_id", nullable = false)
    private Usuario solicitadoPor;

    @ColumnDefault("'COMPLETO'")
    @Lob
    @Column(name = "tipo", nullable = false)
    private String tipo;

    @ColumnDefault("'PENDIENTE'")
    @Lob
    @Column(name = "estado", nullable = false)
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

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
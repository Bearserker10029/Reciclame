package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "normativas", schema = "reciclame_db", indexes = {
        @Index(name = "idx_normativas_tematica",
                columnList = "tematica_id"),
        @Index(name = "idx_normativas_estado",
                columnList = "estado_normativa_id"),
        @Index(name = "idx_normativas_filtro",
                columnList = "ambito, clasificacion, tematica_id, estado_normativa_id, tipo_normativa_id, anio"),
        @Index(name = "idx_normativas_clasificacion",
                columnList = "clasificacion"),
        @Index(name = "idx_normativas_anio",
                columnList = "anio"),
        @Index(name = "idx_normativas_publica",
                columnList = "es_publica")})
public class Normativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Column(name = "descripcion_breve", length = 500)
    private String descripcionBreve;

    @Lob
    @Column(name = "contenido_resumido")
    private String contenidoResumido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tematica_id", nullable = false)
    private Tematica tematica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_normativa_id", nullable = false)
    private TiposNormativa tipoNormativa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_normativa_id", nullable = false)
    private EstadosNormativa estadoNormativa;

    @ColumnDefault("'NACIONAL'")
    @Column(name = "ambito", nullable = false, length = 15)
    private String ambito;

    @ColumnDefault("'VINCULANTE'")
    @Column(name = "clasificacion", nullable = false, length = 20)
    private String clasificacion;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    // ── Auditoría VirusTotal ───────────────────────────────────
    // Sólo se llena cuando el archivo adjunto fue escaneado por VT.
    // Si el archivo es nulo (normativa sólo referenciada por URL externa), quedan NULL.
    @Column(name = "vt_sha256", length = 64)
    private String vtSha256;

    @Column(name = "vt_verdict", length = 20)
    private String vtVerdict;

    @Column(name = "vt_malicious_engines")
    private Integer vtMaliciousEngines;

    @Column(name = "vt_suspicious_engines")
    private Integer vtSuspiciousEngines;

    @Column(name = "vt_total_engines")
    private Integer vtTotalEngines;

    @Column(name = "vt_skipped")
    private Boolean vtSkipped;

    @Column(name = "vt_scanned_at")
    private Instant vtScannedAt;

    @Column(name = "fecha_promulgacion")
    private LocalDate fechaPromulgacion;

    @Column(name = "fecha_entrada_vigencia")
    private LocalDate fechaEntradaVigencia;

    @Column(name = "fecha_derogacion")
    private LocalDate fechaDerogacion;

    @Column(name = "archivo_url", nullable = false, length = 500)
    private String archivoUrl;

    @Column(name = "archivo_tipo_mime", length = 50)
    private String archivoTipoMime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "validada_por_id")
    private Usuario validadaPor;

    @ColumnDefault("0")
    @Column(name = "es_publica", nullable = false)
    private Boolean esPublica;

    @ColumnDefault("0")
    @Column(name = "visualizaciones_count", nullable = false)
    private Integer visualizacionesCount;

    @ColumnDefault("0")
    @Column(name = "descargas_count", nullable = false)
    private Integer descargasCount;

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
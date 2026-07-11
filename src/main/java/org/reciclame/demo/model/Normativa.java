package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300, message = "El título no puede superar los 300 caracteres")
    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Size(max = 500, message = "La descripción breve no puede superar los 500 caracteres")
    @Column(name = "descripcion_breve", length = 500)
    private String descripcionBreve;

    @Lob
    @Column(name = "contenido_resumido")
    private String contenidoResumido;

    @NotNull(message = "La temática es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tematica_id", nullable = false)
    private Tematica tematica;

    @NotNull(message = "El tipo de normativa es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_normativa_id", nullable = false)
    private TiposNormativa tipoNormativa;

    @NotNull(message = "El estado normativo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_normativa_id", nullable = false)
    private EstadosNormativa estadoNormativa;

    @NotBlank(message = "El ámbito es obligatorio")
    @Pattern(regexp = "^(NACIONAL|INTERNACIONAL)$",
            message = "El ámbito debe ser NACIONAL o INTERNACIONAL")
    @ColumnDefault("'NACIONAL'")
    @Column(name = "ambito", nullable = false, length = 15)
    private String ambito;

    @NotBlank(message = "La clasificación es obligatoria")
    @Pattern(regexp = "^(VINCULANTE|NO_VINCULANTE)$",
            message = "La clasificación debe ser VINCULANTE o NO_VINCULANTE")
    @ColumnDefault("'VINCULANTE'")
    @Column(name = "clasificacion", nullable = false, length = 20)
    private String clasificacion;

    @NotNull(message = "El año de publicación es obligatorio")
    @Min(value = 1900, message = "El año debe ser igual o posterior a 1900")
    @Max(value = 2100, message = "El año debe ser igual o anterior a 2100")
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

    @NotBlank(message = "La URL del archivo es obligatoria")
    @Size(max = 500, message = "La URL del archivo no puede superar los 500 caracteres")
    @Column(name = "archivo_url", nullable = false, length = 500)
    private String archivoUrl;

    @Size(max = 255, message = "El tipo MIME no puede superar los 255 caracteres")
    @Column(name = "archivo_tipo_mime", length = 50)
    private String archivoTipoMime;

    @NotNull(message = "El usuario creador es obligatorio")
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
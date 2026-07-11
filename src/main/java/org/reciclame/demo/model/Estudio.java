package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "estudios", schema = "reciclame_db", indexes = {
        @Index(name = "idx_estudios_tematica",
                columnList = "tematica_id"),
        @Index(name = "idx_estudios_anio",
                columnList = "anio"),
        @Index(name = "idx_estudios_publico",
                columnList = "es_publico")})
public class Estudio {
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
    @Column(name = "resumen")
    private String resumen;

    @NotNull(message = "La temática es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tematica_id", nullable = false)
    private Tematica tematica;

    @Size(max = 200, message = "El autor no puede superar los 200 caracteres")
    @Column(name = "autor", length = 200)
    private String autor;

    @Column(name = "fecha_estudio")
    private LocalDate fechaEstudio;

    @Min(value = 1900, message = "El año debe ser igual o posterior a 1900")
    @Max(value = 2100, message = "El año debe ser igual o anterior a 2100")
    @Column(name = "anio")
    private Integer anio;

    @NotBlank(message = "La URL del archivo es obligatoria")
    @Size(max = 500, message = "La URL del archivo no puede superar los 500 caracteres")
    @Column(name = "archivo_url", nullable = false, length = 500)
    private String archivoUrl;

    @Size(max = 255, message = "El tipo MIME no puede superar los 255 caracteres")
    @Column(name = "archivo_tipo_mime", length = 50)
    private String archivoTipoMime;

    // ── Auditoría VirusTotal ───────────────────────────────────
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

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "normativa_relacionada_id")
    private Normativa normativaRelacionada;

    @NotNull(message = "El usuario que subió el estudio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subida_por_id", nullable = false)
    private Usuario subidaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    @ColumnDefault("0")
    @Column(name = "es_publico", nullable = false)
    private Boolean esPublico;

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
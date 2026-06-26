package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EstudioResponseDTO {

    private Long id;
    private String titulo;
    private String descripcionBreve;
    private String resumen;

    // Temática
    private Long tematicaId;
    private String tematicaNombre;
    private String tematicaCodigo;
    private String tematicaColor;

    private String autor;
    private LocalDate fechaEstudio;
    private Integer anio;
    private String archivoUrl;
    private String archivoTipoMime;

    // ── VirusTotal (auditoría de escaneo) ──────────────────────
    private String vtSha256;
    private String vtVerdict;
    private Integer vtMaliciousEngines;
    private Integer vtSuspiciousEngines;
    private Integer vtTotalEngines;
    private Boolean vtSkipped;
    private Instant vtScannedAt;

    // Normativa relacionada (opcional)
    private Long normativaRelacionadaId;
    private String normativaRelacionadaTitulo;

    // Etiquetas many-to-many
    private List<Long> etiquetasIds;
    private List<String> etiquetasNombres;

    private Boolean esPublico;
    private Integer visualizacionesCount;
    private Integer descargasCount;
    private Boolean estadoActivo;
    private Instant createdAt;
}

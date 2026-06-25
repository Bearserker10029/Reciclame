package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class NormativaResponseDTO {

    private Long id;
    private String titulo;
    private String descripcionBreve;
    private String contenidoResumido;

    private Long tematicaId;
    private String tematicaNombre;
    private String tematicaCodigo;
    private String tematicaColor;

    private Long tipoNormativaId;
    private String tipoNormativaNombre;

    private Long estadoNormativaId;
    private String estadoNormativaNombre;

    private String ambito;
    private String clasificacion;
    private Integer anio;

    private LocalDate fechaPromulgacion;
    private LocalDate fechaEntradaVigencia;
    private LocalDate fechaDerogacion;

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

    private Boolean esPublica;
    private Integer visualizacionesCount;
    private Integer descargasCount;
    private Boolean estadoActivo;
    private Instant createdAt;

    private List<Long> etiquetasIds;
    private List<String> etiquetasNombres;
}

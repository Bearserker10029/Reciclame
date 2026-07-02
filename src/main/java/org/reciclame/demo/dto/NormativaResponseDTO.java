package org.reciclame.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.reciclame.demo.helper.EstadoValidacion;

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

    /**
     * Estado de validación del archivo adjunto, derivado de {@code archivo_url} y
     * {@code vt_verdict}. No se persiste: lo calcula {@code EstadoValidacionHelper}.
     * Valores: {@code PENDIENTE}, {@code APROBADO}, {@code RECHAZADO} o {@code null}
     * si el registro no tiene archivo.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private EstadoValidacion estadoValidacion;
}

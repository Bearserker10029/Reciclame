package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class NormativaResponseDTO {

    private Long id;
    private String titulo;
    private String descripcionBreve;
    private String tematicaNombre;
    private String tematicaCodigo;
    private String tematicaColor;
    private String tipoNormativaNombre;
    private String estadoNormativaNombre;
    private String ambito;
    private String clasificacion;
    private Integer anio;
    private LocalDate fechaPromulgacion;
    private LocalDate fechaEntradaVigencia;
    private String archivoUrl;
    private Boolean esPublica;
    private Integer visualizacionesCount;
    private Integer descargasCount;
    private Boolean estadoActivo;
    private Instant createdAt;
}

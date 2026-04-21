package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class EstudioResponseDTO {

    private Long id;
    private String titulo;
    private String descripcionBreve;
    private String resumen;
    private String tematicaNombre;
    private String tematicaCodigo;
    private String tematicaColor;
    private String autor;
    private LocalDate fechaEstudio;
    private Integer anio;
    private String archivoUrl;
    private String archivoTipoMime;
    private Boolean esPublico;
    private Integer visualizacionesCount;
    private Integer descargasCount;
    private Boolean estadoActivo;
    private Instant createdAt;
}

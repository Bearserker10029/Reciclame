package org.reciclame.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EstudioRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300, message = "El título no puede superar los 300 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcionBreve;

    private String resumen;

    @NotNull(message = "Debe seleccionar una temática")
    private Long tematicaId;

    @Size(max = 200, message = "El autor no puede superar los 200 caracteres")
    private String autor;

    private LocalDate fechaEstudio;

    private Integer anio;

    private Boolean esPublico = false;

    private Long normativaRelacionadaId;

    private List<Long> etiquetasIds;
}

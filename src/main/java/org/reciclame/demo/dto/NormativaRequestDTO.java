package org.reciclame.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class NormativaRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300, message = "El título no puede superar los 300 caracteres")
    private String titulo;

    @Size(max = 500, message = "La descripción breve no puede superar los 500 caracteres")
    private String descripcionBreve;

    private String contenidoResumido;

    @NotNull(message = "La temática es obligatoria")
    private Long tematicaId;

    @NotNull(message = "El tipo de normativa es obligatorio")
    private Long tipoNormativaId;

    @NotNull(message = "El estado normativo es obligatorio")
    private Long estadoNormativaId;

    @Pattern(regexp = "^(NACIONAL|INTERNACIONAL)$",
            message = "El ámbito debe ser NACIONAL o INTERNACIONAL")
    private String ambito = "NACIONAL";

    @Pattern(regexp = "^(VINCULANTE|NO_VINCULANTE)$",
            message = "La clasificación debe ser VINCULANTE o NO_VINCULANTE")
    private String clasificacion = "VINCULANTE";

    @NotNull(message = "El año de publicación es obligatorio")
    @Min(value = 1900, message = "El año debe ser igual o posterior a 1900")
    @Max(value = 2100, message = "El año debe ser igual o anterior a 2100")
    private Integer anio;

    private LocalDate fechaPromulgacion;
    private LocalDate fechaEntradaVigencia;
    private LocalDate fechaDerogacion;

    private Boolean esPublica = false;

    private List<Long> etiquetasIds;
}

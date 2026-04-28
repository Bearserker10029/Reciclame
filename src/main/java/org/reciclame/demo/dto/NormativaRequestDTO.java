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
public class NormativaRequestDTO {

    @NotBlank @Size(max = 300)
    private String titulo;

    @Size(max = 500)
    private String descripcionBreve;

    private String contenidoResumido;

    @NotNull
    private Long tematicaId;

    @NotNull
    private Long tipoNormativaId;

    @NotNull
    private Long estadoNormativaId;

    private String ambito = "NACIONAL";

    private String clasificacion = "VINCULANTE";

    @NotNull
    private Integer anio;

    private LocalDate fechaPromulgacion;
    private LocalDate fechaEntradaVigencia;
    private LocalDate fechaDerogacion;

    private Boolean esPublica = false;

    private List<Long> etiquetasIds;
}

package org.reciclame.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtiquetaRequestDTO {

    @NotBlank(message = "El nombre de la etiqueta es obligatorio")
    @Size(max = 50, message = "El nombre de la etiqueta no puede superar los 50 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    private Boolean estadoActivo;
}

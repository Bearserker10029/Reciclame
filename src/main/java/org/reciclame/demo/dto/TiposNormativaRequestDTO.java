package org.reciclame.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TiposNormativaRequestDTO {

    @NotBlank(message = "El nombre del tipo es obligatorio")
    @Size(max = 60, message = "El nombre del tipo no puede superar los 60 caracteres")
    private String nombre;

    private Boolean estadoActivo;
}

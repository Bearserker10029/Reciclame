package org.reciclame.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TematicaRequestDTO {

    @NotBlank(message = "El código de la temática es obligatorio")
    @Pattern(regexp = "^[A-Z0-9_-]{2,10}$",
            message = "El código debe tener entre 2 y 10 caracteres (A-Z, 0-9, _, -)")
    private String codigo;

    @NotBlank(message = "El nombre de la temática es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
            message = "El color debe tener formato hexadecimal (#RRGGBB)")
    private String colorHex;

    private Boolean estadoActivo;
}

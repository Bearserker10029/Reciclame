package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TematicaRequestDTO {
    private String codigo;
    private String nombre;
    private String colorHex;
    private Boolean estadoActivo;
}

package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TematicaResponseDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private String colorHex;
    private Boolean estadoActivo;
}

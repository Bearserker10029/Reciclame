package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean estadoActivo;
}

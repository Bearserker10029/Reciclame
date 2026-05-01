package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EtiquetaRequestDTO {
    private String nombre;
    private String descripcion;
    private Boolean estadoActivo;
}

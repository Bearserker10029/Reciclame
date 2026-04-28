package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParametrosSistemaRequestDTO {
    private String valor;
    private String descripcion;
    private Boolean estadoActivo;
}

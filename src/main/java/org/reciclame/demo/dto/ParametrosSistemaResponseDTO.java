package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ParametrosSistemaResponseDTO {
    private Long id;
    private String clave;
    private String valor;
    private String descripcion;
    private String tipoDato;
    private Boolean estadoActivo;
    private Instant updatedAt;
}

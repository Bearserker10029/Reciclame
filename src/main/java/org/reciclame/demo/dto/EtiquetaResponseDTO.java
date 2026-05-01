package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EtiquetaResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean estadoActivo;
    private Instant createdAt;
}

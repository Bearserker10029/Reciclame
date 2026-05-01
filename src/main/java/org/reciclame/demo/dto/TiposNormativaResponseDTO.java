package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class TiposNormativaResponseDTO {
    private Long id;
    private String nombre;
    private Boolean estadoActivo;
    private Instant createdAt;
}

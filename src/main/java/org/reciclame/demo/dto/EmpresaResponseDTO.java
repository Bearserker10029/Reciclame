package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EmpresaResponseDTO {

    private Long id;
    private String razonSocial;
    private String ruc;
    private String sector;
    private Boolean estadoActivo;
    private Instant createdAt;
}

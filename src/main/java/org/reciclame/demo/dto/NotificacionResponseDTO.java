package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class NotificacionResponseDTO {

    private Long id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String enlaceRecurso;
    private Boolean leida;
    private Instant createdAt;
    private String tiempoRelativo;
}

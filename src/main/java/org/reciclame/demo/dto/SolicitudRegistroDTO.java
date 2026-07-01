package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SolicitudRegistroDTO {

    private Long id;
    private String nombreCompleto;
    private String dni;
    private String correoCorporativo;
    private String empresaNombre;
    private String estado;
    private String motivoRechazo;
    private Instant fechaResolucion;
    private String revisadaPorNombre;
    private Instant createdAt;
}
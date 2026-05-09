package org.reciclame.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocioReporteNotifFila {
    private final String fecha;
    private final String titulo;
    /** "Leída" o "Sin leer" */
    private final String estado;
}

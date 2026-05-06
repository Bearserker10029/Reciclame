package org.reciclame.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocioReporteActividadFila {
    private final String fechaHora;
    private final String titulo;
    /** "Normativa" o "Estudio" */
    private final String tipoEtiqueta;
    private final String detalle;
}

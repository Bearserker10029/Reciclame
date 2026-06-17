package org.reciclame.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta unificada de búsqueda global del topbar.
 * <p>
 * Cada item representa un resultado (normativa / estudio / usuario) con su tipo,
 * un snippet relevante y un link al detalle. La lista de tipos incluidos en la
 * respuesta varía según el rol del usuario que consulta (ver SearchService).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponseDTO {

    /** Texto original consultado, ya saneado. */
    private String query;

    /** Rol efectivo del usuario que consulta (post-impersonación). */
    private String rolEfectivo;

    /** Tipos habilitados para este usuario (ej: ["NORMATIVA","ESTUDIO","USUARIO"]). */
    private List<String> tiposPermitidos;

    /** Resultados encontrados. */
    private List<SearchItemDTO> resultados;

    /** Tiempo total de la consulta (ms). Útil para debug y mostrarlo en la UI. */
    private long tiempoMs;
}

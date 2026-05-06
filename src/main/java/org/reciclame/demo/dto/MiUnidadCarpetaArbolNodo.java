package org.reciclame.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Árbol de carpetas de Mi Unidad para el submenú "Añadir a".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MiUnidadCarpetaArbolNodo(Long id, String nombre, List<MiUnidadCarpetaArbolNodo> children) {
}

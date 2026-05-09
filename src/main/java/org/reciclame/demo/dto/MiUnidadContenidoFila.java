package org.reciclame.demo.dto;

/**
 * Fila unificada para la tabla de Mi Unidad (carpeta o documento).
 */
public record MiUnidadContenidoFila(
        boolean esCarpeta,
        Long carpetaId,
        Long itemId,
        String nombre,
        String propietario,
        String fechaPublicacion,
        String tamanoEtiqueta,
        String documentoTipo,
        Long documentoId,
        long ordenFechaMillis,
        String ordenNombre
) {}

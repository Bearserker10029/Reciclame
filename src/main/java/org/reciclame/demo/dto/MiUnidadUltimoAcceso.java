package org.reciclame.demo.dto;

/**
 * Tarjeta de "Últimos accesos" en Mi Unidad.
 */
public record MiUnidadUltimoAcceso(
        String documentoTipo,
        Long documentoId,
        String titulo,
        String tiempoRelativo,
        String urlVer,
        String urlDescarga,
        String iconVariant
) {}

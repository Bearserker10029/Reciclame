package org.reciclame.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item individual dentro de la respuesta de búsqueda global.
 * El campo {@link #tipo} define qué información se cargó en los campos específicos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchItemDTO {

    /** Tipo de resultado: NORMATIVA | ESTUDIO | USUARIO. */
    private String tipo;

    /** ID de la entidad original (útil para construir el link al detalle). */
    private Long id;

    /** Título principal para mostrar (normativa.titulo / estudio.titulo / usuario.nombreCompleto). */
    private String titulo;

    /** Descripción secundaria / subtítulo / snippet. */
    private String subtitulo;

    /** Snippet corto con el fragmento donde matchea la búsqueda (opcional). */
    private String snippet;

    /** URL relativa para navegar al detalle (ej: "/normativas/visor?id=12"). */
    private String url;

    /** Año (normativa/estudio) — opcional, sirve para mostrar badge. */
    private Integer anio;

    /** Visibilidad (true = público, false = privado/oculto). */
    private Boolean publico;

    /** Ámbito para normativa (NACIONAL/INTERNACIONAL). */
    private String ambito;

    /** Rol para usuarios (ej: SOCIO, ADMINISTRADOR). */
    private String rolNombre;

    /** Correo corporativo para usuarios. */
    private String correo;

    /** Empresa del usuario. */
    private String empresa;
}

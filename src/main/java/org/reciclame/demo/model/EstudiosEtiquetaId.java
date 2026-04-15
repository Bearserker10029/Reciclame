package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode@jakarta.persistence.Embeddable
public class EstudiosEtiquetaId {
@jakarta.persistence.Column(name = "estudio_id", nullable = false)
private java.lang.Long estudioId;

@jakarta.persistence.Column(name = "etiqueta_id", nullable = false)
private java.lang.Long etiquetaId;



}
package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode@jakarta.persistence.Embeddable
public class NormativasEtiquetaId {
@jakarta.persistence.Column(name = "normativa_id", nullable = false)
private java.lang.Long normativaId;

@jakarta.persistence.Column(name = "etiqueta_id", nullable = false)
private java.lang.Long etiquetaId;



}
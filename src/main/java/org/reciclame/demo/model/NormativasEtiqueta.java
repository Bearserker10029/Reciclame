package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "normativas_etiquetas", schema = "reciclame_db")
public class NormativasEtiqueta {
@jakarta.persistence.EmbeddedId
private org.reciclame.demo.model.NormativasEtiquetaId id;

@jakarta.persistence.MapsId("normativaId")
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "normativa_id", nullable = false)
private org.reciclame.demo.model.Normativa normativa;

@jakarta.persistence.MapsId("etiquetaId")
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "etiqueta_id", nullable = false)
private org.reciclame.demo.model.Etiqueta etiqueta;



}
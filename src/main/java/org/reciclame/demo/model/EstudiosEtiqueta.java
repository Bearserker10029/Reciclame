package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "estudios_etiquetas", schema = "reciclame_db")
public class EstudiosEtiqueta {
@jakarta.persistence.EmbeddedId
private org.reciclame.demo.model.EstudiosEtiquetaId id;

@jakarta.persistence.MapsId("estudioId")
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "estudio_id", nullable = false)
private org.reciclame.demo.model.Estudio estudio;

@jakarta.persistence.MapsId("etiquetaId")
@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "etiqueta_id", nullable = false)
private org.reciclame.demo.model.Etiqueta etiqueta;



}
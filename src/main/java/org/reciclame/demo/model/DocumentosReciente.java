package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "documentos_recientes", schema = "reciclame_db")
public class DocumentosReciente {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "usuario_id", nullable = false)
private org.reciclame.demo.model.Usuario usuario;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "documento_tipo", nullable = false)
private java.lang.String documentoTipo;

@jakarta.persistence.Column(name = "documento_id", nullable = false)
private java.lang.Long documentoId;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "ultima_vista", nullable = false)
private java.time.Instant ultimaVista;



}
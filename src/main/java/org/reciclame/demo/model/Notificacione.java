package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "notificaciones", schema = "reciclame_db")
public class Notificacione {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "usuario_id", nullable = false)
private org.reciclame.demo.model.Usuario usuario;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "tipo", nullable = false)
private java.lang.String tipo;

@jakarta.persistence.Column(name = "titulo", nullable = false)
private java.lang.String titulo;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "mensaje", nullable = false)
private java.lang.String mensaje;

@jakarta.persistence.Column(name = "enlace_recurso", length = 500)
private java.lang.String enlaceRecurso;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "leida", nullable = false)
private java.lang.Boolean leida;

@jakarta.persistence.Column(name = "fecha_lectura")
private java.time.Instant fechaLectura;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
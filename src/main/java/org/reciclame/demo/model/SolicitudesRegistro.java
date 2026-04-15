package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "solicitudes_registro", schema = "reciclame_db")
public class SolicitudesRegistro {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.Column(name = "nombre_completo", nullable = false, length = 200)
private java.lang.String nombreCompleto;

@jakarta.persistence.Column(name = "dni", nullable = false, length = 8)
private java.lang.String dni;

@jakarta.persistence.Column(name = "correo_corporativo", nullable = false, length = 200)
private java.lang.String correoCorporativo;

@jakarta.persistence.Column(name = "empresa_nombre", nullable = false, length = 200)
private java.lang.String empresaNombre;

@org.hibernate.annotations.ColumnDefault("'PENDIENTE'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "estado", nullable = false)
private java.lang.String estado;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
@jakarta.persistence.JoinColumn(name = "revisada_por_id")
private org.reciclame.demo.model.Usuario revisadaPor;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "motivo_rechazo")
private java.lang.String motivoRechazo;

@jakarta.persistence.Column(name = "fecha_resolucion")
private java.time.Instant fechaResolucion;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
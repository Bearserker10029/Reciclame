package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "logs_actividad", schema = "reciclame_db")
public class LogsActividad {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "usuario_id", nullable = false)
private org.reciclame.demo.model.Usuario usuario;

@jakarta.persistence.Column(name = "tipo_accion", nullable = false, length = 50)
private java.lang.String tipoAccion;

@jakarta.persistence.Column(name = "recurso_tipo", nullable = false, length = 50)
private java.lang.String recursoTipo;

@jakarta.persistence.Column(name = "recurso_id")
private java.lang.Long recursoId;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "descripcion_detalle")
private java.lang.String descripcionDetalle;

@jakarta.persistence.Column(name = "ip_address", length = 45)
private java.lang.String ipAddress;

@jakarta.persistence.Column(name = "user_agent", length = 500)
private java.lang.String userAgent;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
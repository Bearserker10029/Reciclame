package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "sesiones_impersonacion", schema = "reciclame_db")
public class SesionesImpersonacion {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "superadmin_id", nullable = false)
private org.reciclame.demo.model.Usuario superadmin;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "usuario_objetivo_id", nullable = false)
private org.reciclame.demo.model.Usuario usuarioObjetivo;

@jakarta.persistence.Column(name = "motivo", length = 500)
private java.lang.String motivo;

@jakarta.persistence.Column(name = "ip_address", nullable = false, length = 45)
private java.lang.String ipAddress;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "fecha_inicio", nullable = false)
private java.time.Instant fechaInicio;

@jakarta.persistence.Column(name = "fecha_fin")
private java.time.Instant fechaFin;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "activa", nullable = false)
private java.lang.Boolean activa;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
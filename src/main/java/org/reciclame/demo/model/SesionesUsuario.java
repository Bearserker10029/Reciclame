package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "sesiones_usuario", schema = "reciclame_db")
public class SesionesUsuario {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "usuario_id", nullable = false)
private org.reciclame.demo.model.Usuario usuario;

@jakarta.persistence.Column(name = "token_sesion", nullable = false)
private java.lang.String tokenSesion;

@jakarta.persistence.Column(name = "ip_address", length = 45)
private java.lang.String ipAddress;

@jakarta.persistence.Column(name = "user_agent", length = 500)
private java.lang.String userAgent;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "activa", nullable = false)
private java.lang.Boolean activa;

@jakarta.persistence.Column(name = "fecha_expiracion", nullable = false)
private java.time.Instant fechaExpiracion;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "ultimo_acceso", nullable = false)
private java.time.Instant ultimoAcceso;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
package org.reciclame.demo;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "usuarios", schema = "reciclame_db")
public class Usuario {
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

@jakarta.persistence.Column(name = "password_hash", nullable = false)
private java.lang.String passwordHash;

@jakarta.persistence.Column(name = "cargo", length = 150)
private java.lang.String cargo;

@jakarta.persistence.Column(name = "foto_url", length = 500)
private java.lang.String fotoUrl;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "intentos_fallidos", nullable = false)
private java.lang.Byte intentosFallidos;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "cuenta_bloqueada", nullable = false)
private java.lang.Boolean cuentaBloqueada;

@jakarta.persistence.Column(name = "ip_bloqueada", length = 45)
private java.lang.String ipBloqueada;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "tfa_activo", nullable = false)
private java.lang.Boolean tfaActivo;

@jakarta.persistence.Column(name = "tfa_secret", length = 64)
private java.lang.String tfaSecret;

@org.hibernate.annotations.ColumnDefault("'PENDIENTE'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "estado_cuenta", nullable = false)
private java.lang.String estadoCuenta;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@jakarta.persistence.Column(name = "ultimo_login")
private java.time.Instant ultimoLogin;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "updated_at", nullable = false)
private java.time.Instant updatedAt;



}
package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "preferencias_notificacion", schema = "reciclame_db")
public class PreferenciasNotificacion {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.OneToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "usuario_id", nullable = false)
private org.reciclame.demo.model.Usuario usuario;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "email_habilitado", nullable = false)
private java.lang.Boolean emailHabilitado;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "push_habilitado", nullable = false)
private java.lang.Boolean pushHabilitado;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "sms_habilitado", nullable = false)
private java.lang.Boolean smsHabilitado;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "notif_nuevas_normativas", nullable = false)
private java.lang.Boolean notifNuevasNormativas;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "notif_nuevos_estudios", nullable = false)
private java.lang.Boolean notifNuevosEstudios;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "notif_cambios_seguidos", nullable = false)
private java.lang.Boolean notifCambiosSeguidos;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "notif_mensajes_admin", nullable = false)
private java.lang.Boolean notifMensajesAdmin;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "notif_digesto_semanal", nullable = false)
private java.lang.Boolean notifDigestoSemanal;

@org.hibernate.annotations.ColumnDefault("'DIARIA'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "frecuencia_email", nullable = false)
private java.lang.String frecuenciaEmail;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "updated_at", nullable = false)
private java.time.Instant updatedAt;



}
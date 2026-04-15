package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "preferencias_notificacion", schema = "reciclame_db")
public class PreferenciasNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ColumnDefault("1")
    @Column(name = "email_habilitado", nullable = false)
    private Boolean emailHabilitado;

    @ColumnDefault("0")
    @Column(name = "push_habilitado", nullable = false)
    private Boolean pushHabilitado;

    @ColumnDefault("0")
    @Column(name = "sms_habilitado", nullable = false)
    private Boolean smsHabilitado;

    @ColumnDefault("1")
    @Column(name = "notif_nuevas_normativas", nullable = false)
    private Boolean notifNuevasNormativas;

    @ColumnDefault("1")
    @Column(name = "notif_nuevos_estudios", nullable = false)
    private Boolean notifNuevosEstudios;

    @ColumnDefault("1")
    @Column(name = "notif_cambios_seguidos", nullable = false)
    private Boolean notifCambiosSeguidos;

    @ColumnDefault("1")
    @Column(name = "notif_mensajes_admin", nullable = false)
    private Boolean notifMensajesAdmin;

    @ColumnDefault("0")
    @Column(name = "notif_digesto_semanal", nullable = false)
    private Boolean notifDigestoSemanal;

    @ColumnDefault("'DIARIA'")
    @Lob
    @Column(name = "frecuencia_email", nullable = false)
    private String frecuenciaEmail;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


}
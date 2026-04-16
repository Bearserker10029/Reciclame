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
@Table(name = "notificaciones", schema = "reciclame_db", indexes = {
        @Index(name = "idx_notificaciones_usuario",
                columnList = "usuario_id"),
        @Index(name = "idx_notificaciones_leida",
                columnList = "leida"),
        @Index(name = "idx_notificaciones_fecha",
                columnList = "created_at")})
public class Notificacione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Lob
    @Column(name = "mensaje", nullable = false)
    private String mensaje;

    @Column(name = "enlace_recurso", length = 500)
    private String enlaceRecurso;

    @ColumnDefault("0")
    @Column(name = "leida", nullable = false)
    private Boolean leida;

    @Column(name = "fecha_lectura")
    private Instant fechaLectura;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "chatbot_feedback", schema = "reciclame_db",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_feedback_mensaje_usuario",
                        columnNames = {"mensaje_id", "usuario_id"})
        },
        indexes = {
                @Index(name = "idx_feedback_usuario", columnList = "usuario_id"),
                @Index(name = "idx_feedback_calificacion", columnList = "calificacion")
        })
public class ChatbotFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private MensajesChatbot mensaje;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** 1 = util, -1 = no util */
    @Column(name = "calificacion", nullable = false)
    private Short calificacion;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

}
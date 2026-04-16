package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mensajes_chatbot", schema = "reciclame_db")
public class MensajesChatbot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private ConversacionesChatbot conversacion;

    @Lob
    @Column(name = "rol", nullable = false)
    private String rol;

    @Lob
    @Column(name = "contenido", nullable = false)
    private String contenido;

    @Column(name = "intent_detectado", length = 100)
    private String intentDetectado;

    @Column(name = "confianza", precision = 5, scale = 4)
    private BigDecimal confianza;

    @ColumnDefault("0")
    @Column(name = "derivado_soporte", nullable = false)
    private Boolean derivadoSoporte;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
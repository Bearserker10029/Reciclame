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
@Table(name = "tokens_acceso", schema = "reciclame_db", indexes = {
        @Index(name = "idx_tokens_usuario",
                columnList = "usuario_id"),
        @Index(name = "idx_tokens_expiracion",
                columnList = "fecha_expiracion")}, uniqueConstraints = {@UniqueConstraint(name = "token",
        columnNames = {"token"})})
public class TokensAcceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id")
    private Usuarios usuarios;

    @Column(name = "token", nullable = false)
    private String token;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @ColumnDefault("0")
    @Column(name = "usado", nullable = false)
    private Boolean usado;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
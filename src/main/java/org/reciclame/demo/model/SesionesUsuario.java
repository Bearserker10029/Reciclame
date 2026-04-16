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
@Table(name = "sesiones_usuario", schema = "reciclame_db")
public class SesionesUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_sesion", nullable = false)
    private String tokenSesion;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @ColumnDefault("1")
    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ultimo_acceso", nullable = false)
    private Instant ultimoAcceso;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
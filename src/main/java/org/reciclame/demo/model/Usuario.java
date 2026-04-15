package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "usuarios", schema = "reciclame_db")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "dni", nullable = false, length = 8)
    private String dni;

    @Column(name = "correo_corporativo", nullable = false, length = 200)
    private String correoCorporativo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "cargo", length = 150)
    private String cargo;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @ColumnDefault("0")
    @Column(name = "intentos_fallidos", nullable = false)
    private Byte intentosFallidos;

    @ColumnDefault("0")
    @Column(name = "cuenta_bloqueada", nullable = false)
    private Boolean cuentaBloqueada;

    @Column(name = "ip_bloqueada", length = 45)
    private String ipBloqueada;

    @ColumnDefault("0")
    @Column(name = "tfa_activo", nullable = false)
    private Boolean tfaActivo;

    @Column(name = "tfa_secret", length = 64)
    private String tfaSecret;

    @ColumnDefault("'PENDIENTE'")
    @Lob
    @Column(name = "estado_cuenta", nullable = false)
    private String estadoCuenta;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


}
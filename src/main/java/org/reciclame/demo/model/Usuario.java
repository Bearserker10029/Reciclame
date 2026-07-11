package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "usuarios", schema = "reciclame_db", indexes = {
        @Index(name = "idx_usuarios_dni",
                columnList = "dni"),
        @Index(name = "idx_usuarios_correo",
                columnList = "correo_corporativo"),
        @Index(name = "idx_usuarios_rol",
                columnList = "rol_id"),
        @Index(name = "idx_usuarios_empresa",
                columnList = "empresa_id"),
        @Index(name = "idx_usuarios_estado",
                columnList = "estado_cuenta")}, uniqueConstraints = {
        @UniqueConstraint(name = "dni",
                columnNames = {"dni"}),
        @UniqueConstraint(name = "correo_corporativo",
                columnNames = {"correo_corporativo"})})
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 200, message = "El nombre completo no puede superar los 200 caracteres")
    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    @Column(name = "dni", nullable = false, length = 8)
    private String dni;

    @NotBlank(message = "El correo corporativo es obligatorio")
    @Email(message = "El correo corporativo debe tener un formato válido")
    @Size(max = 200, message = "El correo corporativo no puede superar los 200 caracteres")
    @Column(name = "correo_corporativo", nullable = false, length = 200)
    private String correoCorporativo;

    @NotBlank(message = "El password es obligatorio")
    @Size(max = 255, message = "El password hash no puede superar los 255 caracteres")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 150, message = "El cargo no puede superar los 150 caracteres")
    @Column(name = "cargo", length = 150)
    private String cargo;

    @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @NotNull(message = "El rol es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Min(value = 0, message = "Los intentos fallidos no pueden ser negativos")
    @Max(value = 255, message = "Los intentos fallidos no pueden superar 255")
    @ColumnDefault("0")
    @Column(name = "intentos_fallidos", nullable = false)
    private Byte intentosFallidos;

    @ColumnDefault("0")
    @Column(name = "cuenta_bloqueada", nullable = false)
    private Boolean cuentaBloqueada;

    @Column(name = "fecha_bloqueo")
    private Instant fechaBloqueo;

    @Size(max = 45, message = "La IP bloqueada no puede superar los 45 caracteres")
    @Column(name = "ip_bloqueada", length = 45)
    private String ipBloqueada;

    @ColumnDefault("0")
    @Column(name = "tfa_activo", nullable = false)
    private Boolean tfaActivo;

    @Size(max = 64, message = "El secreto TFA no puede superar los 64 caracteres")
    @Column(name = "tfa_secret", length = 64)
    private String tfaSecret;

    @NotBlank(message = "El estado de cuenta es obligatorio")
    @Pattern(regexp = "^(PENDIENTE|ACTIVO|INACTIVO|BLOQUEADO)$",
            message = "El estado de cuenta debe ser PENDIENTE, ACTIVO, INACTIVO o BLOQUEADO")
    @ColumnDefault("'PENDIENTE'")
    @Column(name = "estado_cuenta", nullable = false, length = 20)
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
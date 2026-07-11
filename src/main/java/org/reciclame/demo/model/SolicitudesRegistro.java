package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
@Table(name = "solicitudes_registro", schema = "reciclame_db", indexes = {
        @Index(name = "idx_solicitudes_correo",
                columnList = "correo_corporativo"),
        @Index(name = "idx_solicitudes_estado",
                columnList = "estado")})
public class SolicitudesRegistro {
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

    @Size(max = 200, message = "El nombre de la empresa no puede superar los 200 caracteres")
    @Column(name = "empresa_nombre", length = 200)
    private String empresaNombre;

    @NotNull(message = "El estado de la solicitud es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "revisada_por_id")
    private Usuario revisadaPor;

    @Lob
    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
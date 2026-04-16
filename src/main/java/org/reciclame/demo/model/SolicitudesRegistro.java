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

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "dni", nullable = false, length = 8)
    private String dni;

    @Column(name = "correo_corporativo", nullable = false, length = 200)
    private String correoCorporativo;

    @Column(name = "empresa_nombre", nullable = false, length = 200)
    private String empresaNombre;

    @ColumnDefault("'PENDIENTE'")
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

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
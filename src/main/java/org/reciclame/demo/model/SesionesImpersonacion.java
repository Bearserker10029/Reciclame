package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "sesiones_impersonacion", schema = "reciclame_db", indexes = {
        @Index(name = "idx_impersonacion_sa",
                columnList = "superadmin_id"),
        @Index(name = "idx_impersonacion_objetivo",
                columnList = "usuario_objetivo_id"),
        @Index(name = "idx_impersonacion_activa",
                columnList = "activa")})
public class SesionesImpersonacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "superadmin_id", nullable = false)
    private Usuarios superadmin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_objetivo_id", nullable = false)
    private Usuarios usuariosObjetivo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin")
    private Instant fechaFin;

    @ColumnDefault("1")
    @Column(name = "activa", nullable = false)
    private Boolean activa;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
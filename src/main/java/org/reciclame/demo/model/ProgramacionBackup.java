package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "programaciones_backup", schema = "reciclame_db")
public class ProgramacionBackup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ColumnDefault("'COMPLETO'")
    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Column(name = "frecuencia", nullable = false, length = 15)
    private String frecuencia;

    @Column(name = "hora_ejecucion", nullable = false)
    private LocalTime horaEjecucion;

    @Column(name = "dia_semana")
    private Integer diaSemana;

    @Column(name = "dia_mes")
    private Integer diaMes;

    @ColumnDefault("1")
    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (estadoActivo == null) {
            estadoActivo = true;
        }
        if (habilitado == null) {
            habilitado = true;
        }
        if (horaEjecucion == null) {
            horaEjecucion = LocalTime.of(2, 0);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
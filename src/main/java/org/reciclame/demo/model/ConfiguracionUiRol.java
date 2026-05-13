package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_ui_rol", schema = "reciclame_db",
        uniqueConstraints = @UniqueConstraint(name = "uq_ui_rol_clave", columnNames = {"rol", "clave"}))
public class ConfiguracionUiRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    /** Valores separados por coma, p.ej. "nombre,codigo,anio,acciones" */
    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    @Column(name = "actualizado_at")
    private Instant actualizadoAt;

    public ConfiguracionUiRol(String rol, String clave, String valor) {
        this.rol = rol;
        this.clave = clave;
        this.valor = valor;
        this.actualizadoAt = Instant.now();
    }
}

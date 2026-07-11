package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "roles", schema = "reciclame_db", uniqueConstraints = {@UniqueConstraint(name = "nombre",
        columnNames = {"nombre"})})
public class Rol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 30, message = "El nombre del rol no puede superar los 30 caracteres")
    @Pattern(regexp = "^(SUPERADMIN|ADMINISTRADOR|SOCIO|VISUALIZADOR)$",
            message = "El nombre del rol debe ser SUPERADMIN, ADMINISTRADOR, SOCIO o VISUALIZADOR")
    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    @Size(max = 255, message = "La descripción del rol no puede superar los 255 caracteres")
    @Column(name = "descripcion")
    private String descripcion;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "estados_normativa", schema = "reciclame_db", uniqueConstraints = {@UniqueConstraint(name = "nombre",
        columnNames = {"nombre"})})
public class EstadosNormativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(max = 60, message = "El nombre del estado no puede superar los 60 caracteres")
    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
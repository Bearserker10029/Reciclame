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
@Table(name = "empresas", schema = "reciclame_db", indexes = {@Index(name = "idx_empresas_razon_social",
        columnList = "razon_social")}, uniqueConstraints = {@UniqueConstraint(name = "ruc",
        columnNames = {"ruc"})})
public class Empresa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200, message = "La razón social no puede superar los 200 caracteres")
    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener exactamente 11 dígitos numéricos")
    @Size(max = 11, message = "El RUC no puede superar los 11 caracteres")
    @Column(name = "ruc", length = 11)
    private String ruc;

    @Size(max = 100, message = "El sector no puede superar los 100 caracteres")
    @Column(name = "sector", length = 100)
    private String sector;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


}
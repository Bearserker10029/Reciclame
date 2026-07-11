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
@Table(name = "tematicas", schema = "reciclame_db", uniqueConstraints = {@UniqueConstraint(name = "codigo",
        columnNames = {"codigo"})})
public class Tematica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "El código de la temática es obligatorio")
    @Pattern(regexp = "^[A-Z0-9_-]{2,10}$",
            message = "El código debe tener entre 2 y 10 caracteres (A-Z, 0-9, _, -)")
    @Column(name = "codigo", nullable = false, length = 10)
    private String codigo;

    @NotBlank(message = "El nombre de la temática es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
            message = "El color debe tener formato hexadecimal (#RRGGBB)")
    @ColumnDefault("'#95A5A6'")
    @Column(name = "color_hex", nullable = false, length = 7)
    private String colorHex;

    @ColumnDefault("1")
    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
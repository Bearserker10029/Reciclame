package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "parametros_sistema", schema = "reciclame_db", indexes = {@Index(name = "idx_parametros_clave",
        columnList = "clave")}, uniqueConstraints = {@UniqueConstraint(name = "clave",
        columnNames = {"clave"})})
public class ParametrosSistema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "La clave del parámetro es obligatoria")
    @Size(max = 100, message = "La clave no puede superar los 100 caracteres")
    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @NotBlank(message = "El valor del parámetro es obligatorio")
    @Lob
    @Column(name = "valor", nullable = false)
    private String valor;

    @Size(max = 300, message = "La descripción no puede superar los 300 caracteres")
    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @NotBlank(message = "El tipo de dato es obligatorio")
    @Pattern(regexp = "^(STRING|INTEGER|BOOLEAN|JSON)$",
            message = "El tipo de dato debe ser STRING, INTEGER, BOOLEAN o JSON")
    @ColumnDefault("'STRING'")
    @Column(name = "tipo_dato", nullable = false, length = 10)
    private String tipoDato;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "modificado_por_id")
    private Usuario modificadoPor;

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
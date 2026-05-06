package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "unidad_carpetas", schema = "reciclame_db", indexes = {
        @Index(name = "idx_unidad_carpeta_usuario", columnList = "usuario_id"),
        @Index(name = "idx_unidad_carpeta_parent", columnList = "parent_id")
})
public class MiUnidadCarpeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MiUnidadCarpeta parent;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

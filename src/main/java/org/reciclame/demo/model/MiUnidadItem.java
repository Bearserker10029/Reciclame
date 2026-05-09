package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "unidad_items", schema = "reciclame_db", indexes = {
        @Index(name = "idx_unidad_item_usuario", columnList = "usuario_id"),
        @Index(name = "idx_unidad_item_carpeta", columnList = "carpeta_id"),
        @Index(name = "idx_unidad_item_doc", columnList = "documento_tipo,documento_id")
})
public class MiUnidadItem {

    public static final String TIPO_NORMATIVA = "NORMATIVA";
    public static final String TIPO_ESTUDIO = "ESTUDIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carpeta_id")
    private MiUnidadCarpeta carpeta;

    @Column(name = "documento_tipo", nullable = false, length = 20)
    private String documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    /** Título que ve el usuario en Mi Unidad (p. ej. el de la tabla normativo/estudios). */
    @Column(name = "titulo_presentacion", length = 500)
    private String tituloPresentacion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

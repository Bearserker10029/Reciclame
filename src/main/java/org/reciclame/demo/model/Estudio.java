package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "estudios", schema = "reciclame_db", indexes = {
        @Index(name = "idx_estudios_tematica",
                columnList = "tematica_id"),
        @Index(name = "idx_estudios_anio",
                columnList = "anio"),
        @Index(name = "idx_estudios_publico",
                columnList = "es_publico")})
public class Estudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Column(name = "descripcion_breve", length = 500)
    private String descripcionBreve;

    @Lob
    @Column(name = "resumen")
    private String resumen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tematica_id", nullable = false)
    private Tematica tematica;

    @Column(name = "autor", length = 200)
    private String autor;

    @Column(name = "fecha_estudio")
    private LocalDate fechaEstudio;

    @Column(name = "anio")
    private Integer anio;

    @Column(name = "archivo_url", nullable = false, length = 500)
    private String archivoUrl;

    @Column(name = "archivo_tipo_mime", length = 50)
    private String archivoTipoMime;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "normativa_relacionada_id")
    private Normativa normativaRelacionada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subida_por_id", nullable = false)
    private Usuario subidaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    @ColumnDefault("0")
    @Column(name = "es_publico", nullable = false)
    private Boolean esPublico;

    @ColumnDefault("0")
    @Column(name = "visualizaciones_count", nullable = false)
    private Integer visualizacionesCount;

    @ColumnDefault("0")
    @Column(name = "descargas_count", nullable = false)
    private Integer descargasCount;

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
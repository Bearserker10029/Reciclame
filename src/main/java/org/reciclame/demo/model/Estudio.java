package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "estudios", schema = "reciclame_db")
public class Estudio {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.Column(name = "titulo", nullable = false, length = 300)
private java.lang.String titulo;

@jakarta.persistence.Column(name = "descripcion_breve", length = 500)
private java.lang.String descripcionBreve;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "resumen")
private java.lang.String resumen;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "tematica_id", nullable = false)
private org.reciclame.demo.model.tematicas tematica;

@jakarta.persistence.Column(name = "autor", length = 200)
private java.lang.String autor;

@jakarta.persistence.Column(name = "fecha_estudio")
private java.time.LocalDate fechaEstudio;

@jakarta.persistence.Column(name = "anio")
private java.lang.Integer anio;

@jakarta.persistence.Column(name = "archivo_url", nullable = false, length = 500)
private java.lang.String archivoUrl;

@jakarta.persistence.Column(name = "archivo_tipo_mime", length = 50)
private java.lang.String archivoTipoMime;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
@jakarta.persistence.JoinColumn(name = "normativa_relacionada_id")
private org.reciclame.demo.model.Normativa normativaRelacionada;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "subida_por_id", nullable = false)
private org.reciclame.demo.model.Usuario subidaPor;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
@jakarta.persistence.JoinColumn(name = "validado_por_id")
private org.reciclame.demo.model.Usuario validadoPor;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "es_publico", nullable = false)
private java.lang.Boolean esPublico;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "visualizaciones_count", nullable = false)
private java.lang.Integer visualizacionesCount;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "descargas_count", nullable = false)
private java.lang.Integer descargasCount;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "updated_at", nullable = false)
private java.time.Instant updatedAt;



}
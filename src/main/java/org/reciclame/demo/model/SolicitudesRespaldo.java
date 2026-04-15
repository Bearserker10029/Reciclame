package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "solicitudes_respaldo", schema = "reciclame_db")
public class SolicitudesRespaldo {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "solicitado_por_id", nullable = false)
private org.reciclame.demo.model.Usuario solicitadoPor;

@org.hibernate.annotations.ColumnDefault("'COMPLETO'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "tipo", nullable = false)
private java.lang.String tipo;

@org.hibernate.annotations.ColumnDefault("'PENDIENTE'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "estado", nullable = false)
private java.lang.String estado;

@jakarta.persistence.Column(name = "storage_key", length = 500)
private java.lang.String storageKey;

@jakarta.persistence.Column(name = "tamanio_bytes")
private java.lang.Long tamanioBytes;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "notas")
private java.lang.String notas;

@jakarta.persistence.Column(name = "fecha_completado")
private java.time.Instant fechaCompletado;

@org.hibernate.annotations.ColumnDefault("1")
@jakarta.persistence.Column(name = "estado_activo", nullable = false)
private java.lang.Boolean estadoActivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
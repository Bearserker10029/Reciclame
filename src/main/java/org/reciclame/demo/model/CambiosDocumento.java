package org.reciclame.demo;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "cambios_documento", schema = "reciclame_db")
public class CambiosDocumento {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "documento_tipo", nullable = false)
private java.lang.String documentoTipo;

@jakarta.persistence.Column(name = "documento_id", nullable = false)
private java.lang.Long documentoId;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@jakarta.persistence.JoinColumn(name = "modificado_por_id", nullable = false)
private org.reciclame.demo.Usuario modificadoPor;

@jakarta.persistence.Column(name = "tipo_cambio", nullable = false, length = 50)
private java.lang.String tipoCambio;

@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@jakarta.persistence.Column(name = "campos_antes")
private java.util.Map<java.lang.String, java.lang.Object> camposAntes;

@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
@jakarta.persistence.Column(name = "campos_despues")
private java.util.Map<java.lang.String, java.lang.Object> camposDespues;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "motivo")
private java.lang.String motivo;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "parametros_sistema", schema = "reciclame_db")
public class ParametrosSistema {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.Column(name = "clave", nullable = false, length = 100)
private java.lang.String clave;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "valor", nullable = false)
private java.lang.String valor;

@jakarta.persistence.Column(name = "descripcion", length = 300)
private java.lang.String descripcion;

@org.hibernate.annotations.ColumnDefault("'STRING'")
@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "tipo_dato", nullable = false)
private java.lang.String tipoDato;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.SET_NULL)
@jakarta.persistence.JoinColumn(name = "modificado_por_id")
private org.reciclame.demo.model.Usuario modificadoPor;

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
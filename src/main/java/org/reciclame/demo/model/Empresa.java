package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "empresas", schema = "reciclame_db")
public class Empresa {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.Column(name = "razon_social", nullable = false, length = 200)
private java.lang.String razonSocial;

@jakarta.persistence.Column(name = "ruc", length = 11)
private java.lang.String ruc;

@jakarta.persistence.Column(name = "sector", length = 100)
private java.lang.String sector;

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
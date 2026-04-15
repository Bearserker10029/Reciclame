package org.reciclame.demo.model;

@lombok.Getter
@lombok.Setter@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "mensajes_chatbot", schema = "reciclame_db")
public class MensajesChatbot {
@jakarta.persistence.Id
@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
@jakarta.persistence.Column(name = "id", nullable = false)
private java.lang.Long id;

@jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
@org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
@jakarta.persistence.JoinColumn(name = "conversacion_id", nullable = false)
private org.reciclame.demo.model.conversaciones_chatbot conversacion;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "rol", nullable = false)
private java.lang.String rol;

@jakarta.persistence.Lob
@jakarta.persistence.Column(name = "contenido", nullable = false)
private java.lang.String contenido;

@jakarta.persistence.Column(name = "intent_detectado", length = 100)
private java.lang.String intentDetectado;

@jakarta.persistence.Column(name = "confianza", precision = 5, scale = 4)
private java.math.BigDecimal confianza;

@org.hibernate.annotations.ColumnDefault("0")
@jakarta.persistence.Column(name = "derivado_soporte", nullable = false)
private java.lang.Boolean derivadoSoporte;

@org.hibernate.annotations.ColumnDefault("CURRENT_TIMESTAMP")
@jakarta.persistence.Column(name = "created_at", nullable = false)
private java.time.Instant createdAt;



}
package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="conversaciones_chatbot")
public class conversaciones_chatbot {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private String id;
    private BigInteger usuario_id;
    private String idioma;
    private String resumen;
    private Boolean estado_activo;
    private Timestamp created_at;
    private Timestamp updated_at;

}

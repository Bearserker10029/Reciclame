package org.reciclame.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="tipos_normativa")
public class TiposNormativa {
    @Id
    private String id;
    private String nombre;
    private boolean estado_activo;
    private Timestamp created_at;
}

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
@Table(name="tematicas")
public class Tematicas {
    @Id
    private String id;
    private String codigo;
    private String nombre;
    private String color_hex;
    private boolean estado_activo;
    private Timestamp created_at;
}

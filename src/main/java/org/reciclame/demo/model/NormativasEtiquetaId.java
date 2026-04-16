package org.reciclame.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class NormativasEtiquetaId implements Serializable {
    private static final long serialVersionUID = 85958363736493503L;
    @Column(name = "normativa_id", nullable = false)
    private Long normativaId;

    @Column(name = "etiqueta_id", nullable = false)
    private Long etiquetaId;


}
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
public class EstudiosEtiquetaId implements Serializable {
    private static final long serialVersionUID = 8534484980225814555L;
    @Column(name = "estudio_id", nullable = false)
    private Long estudioId;

    @Column(name = "etiqueta_id", nullable = false)
    private Long etiquetaId;


}
package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "estudios_etiquetas", schema = "reciclame_db")
public class EstudiosEtiquetas {
    @EmbeddedId
    private EstudiosEtiquetaId id;

    @MapsId("estudioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "estudio_id", nullable = false)
    private Estudios estudios;

    @MapsId("etiquetaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "etiqueta_id", nullable = false)
    private Etiquetas etiquetas;


}
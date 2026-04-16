package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "normativas_etiquetas", schema = "reciclame_db")
public class NormativasEtiqueta {
    @EmbeddedId
    private NormativasEtiquetaId id;

    @MapsId("normativaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "normativa_id", nullable = false)
    private Normativas normativas;

    @MapsId("etiquetaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "etiqueta_id", nullable = false)
    private Etiquetas etiquetas;


}
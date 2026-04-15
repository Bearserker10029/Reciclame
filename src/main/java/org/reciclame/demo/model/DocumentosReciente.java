package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "documentos_recientes", schema = "reciclame_db")
public class DocumentosReciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Lob
    @Column(name = "documento_tipo", nullable = false)
    private String documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ultima_vista", nullable = false)
    private Instant ultimaVista;


}
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
@Table(name = "documentos_recientes", schema = "reciclame_db", indexes = {
        @Index(name = "idx_recientes_usuario",
                columnList = "usuario_id"),
        @Index(name = "idx_recientes_fecha",
                columnList = "ultima_vista")}, uniqueConstraints = {@UniqueConstraint(name = "uq_recientes_usuario_doc",
        columnNames = {
                "usuario_id",
                "documento_tipo",
                "documento_id"})})
public class DocumentosRecientes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuarios;

    @Column(name = "documento_tipo", nullable = false, length = 20)
    private String documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "ultima_vista", nullable = false)
    private Instant ultimaVista;


}
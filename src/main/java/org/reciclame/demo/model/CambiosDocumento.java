package org.reciclame.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "cambios_documento", schema = "reciclame_db")
public class CambiosDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "documento_tipo", nullable = false)
    private String documentoTipo;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modificado_por_id", nullable = false)
    private Usuario modificadoPor;

    @Column(name = "tipo_cambio", nullable = false, length = 50)
    private String tipoCambio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campos_antes")
    private Map<String, Object> camposAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campos_despues")
    private Map<String, Object> camposDespues;

    @Lob
    @Column(name = "motivo")
    private String motivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


}
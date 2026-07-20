package org.reciclame.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Cola de archivos de MinIO que deben purgarse después de un período de gracia (72h por defecto).
 * <p>
 * Flujo:
 * <ol>
 *   <li>Al reemplazar/eliminar un estudio o normativa, se llama a
 *       {@code MinioPendingDeletionService.programarEliminacion(...)} en lugar de borrar
 *       inmediatamente.</li>
 *   <li>El archivo se registra en esta tabla con una {@code fechaProgramada} = now + TTL.</li>
 *   <li>{@code MinioCleanupScheduler} corre cada hora, busca los registros con
 *       {@code fechaProgramada <= now}, los borra de MinIO y marca el registro como purgado.</li>
 *   <li>Durante las 72h, si el admin quiere recuperar el archivo (caso de reemplazo accidental),
 *       puede llamar a {@code cancelarEliminacion(objectName)} antes de que el scheduler lo procese.</li>
 * </ol>
 * El campo {@code objectName} es la clave única lógica: si dos eventos referencian el mismo
 * objectName (ej. doble click en "Eliminar"), el segundo debe no-opear o reemplazar.
 * </p>
 */
@Getter
@Setter
@Entity
@Table(name = "minio_pending_deletion", schema = "reciclame_db", indexes = {
        @Index(name = "idx_minio_pending_fecha", columnList = "fecha_programada"),
        @Index(name = "idx_minio_pending_estado", columnList = "estado"),
        @Index(name = "idx_minio_pending_object", columnList = "object_name")})
public class MinioPendingDeletion {

    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_PURGADO   = "PURGADO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_FALLIDO   = "FALLIDO";

    public static final String TIPO_ESTUDIO    = "ESTUDIO";
    public static final String TIPO_NORMATIVA  = "NORMATIVA";

    public static final String MOTIVO_REEMPLAZO   = "REEMPLAZO";
    public static final String MOTIVO_ELIMINACION = "ELIMINACION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotBlank(message = "El objectName es obligatorio")
    @Size(max = 500)
    @Column(name = "object_name", nullable = false, length = 500)
    private String objectName;

    @Size(max = 500)
    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @NotBlank
    @Pattern(regexp = "^(ESTUDIO|NORMATIVA)$")
    @Column(name = "documento_tipo", nullable = false, length = 20)
    private String documentoTipo;

    @Column(name = "documento_id")
    private Long documentoId;

    @NotBlank
    @Pattern(regexp = "^(REEMPLAZO|ELIMINACION)$")
    @Column(name = "motivo", nullable = false, length = 20)
    private String motivo;

    /**
     * Instante en que el scheduler debe purgar el archivo.
     * Por defecto = createdAt + 72h. Configurable vía {@code minio.pending-deletion.ttl-hours}.
     */
    @NotNull
    @Column(name = "fecha_programada", nullable = false)
    private Instant fechaProgramada;

    @NotBlank
    @Pattern(regexp = "^(PENDIENTE|PURGADO|CANCELADO|FALLIDO)$")
    @ColumnDefault("'PENDIENTE'")
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "purgado_at")
    private Instant purgadoAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "intentos", nullable = false)
    @ColumnDefault("0")
    private Integer intentos = 0;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (estado == null) estado = ESTADO_PENDIENTE;
        if (intentos == null) intentos = 0;
    }
}

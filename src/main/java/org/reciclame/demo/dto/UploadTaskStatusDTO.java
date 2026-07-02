package org.reciclame.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.reciclame.demo.helper.EstadoValidacion;

import java.time.Instant;

/**
 * Estado actual de validación de un archivo subido vía el flujo de staging asíncrono.
 *
 * <p>El frontend (listas de normativas/estudios) hace polling a
 * {@code GET /api/uploads/{tipo}/{taskId}} hasta que {@link #estado} sea
 * {@code APROBADO}, {@code RECHAZADO} o {@code ERROR}.</p>
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadTaskStatusDTO {

    /** Tipo del recurso afectado: "NORMATIVA" o "ESTUDIO". */
    private String tipo;

    /** Identificador del staging object (uuid). */
    private String taskId;

    /** ID del registro en BD (normativa.id o estudio.id). */
    private Long refId;

    /** Estado actual: PENDIENTE, APROBADO, RECHAZADO o ERROR. */
    private EstadoValidacion estado;

    /** URL definitiva en MinIO (presente cuando APROBADO). */
    private String archivoUrl;

    /** Veredicto VT (CLEAN/UNDETECTED/MALICIOUS/SUSPICIOUS/SKIPPED). */
    private String vtVerdict;

    /** Motivo de error o skip. */
    private String message;

    /** Cuándo se actualizó por última vez. */
    private Instant updatedAt;
}

package org.reciclame.demo.config;

import org.reciclame.demo.exception.VirusTotalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Advice global para que cualquier excepción de Spring Boot o el contenedor Servlet
 * devuelva JSON en lugar de una página HTML por defecto. Esto es crítico para que
 * los endpoints REST (subida de archivos, AJAX en general) no rompan el frontend
 * con errores tipo "Unexpected token '<', \"<html> <h\"... is not valid JSON".
 *
 * <p>Cubre los casos más comunes de subida de archivos pesados:</p>
 * <ul>
 *   <li>{@link MaxUploadSizeExceededException} → 413 con mensaje claro.</li>
 *   <li>Excepciones genéricas → 500 con detalle del error.</li>
 * </ul>
 *
 * <p><b>¿Por qué este advice?</b> Spring Boot por defecto delega los errores al
 * mecanismo de páginas de error (BasicErrorController), que devuelve HTML cuando
 * el cliente no envió {@code Accept: application/json}. En producción, donde el
 * proxy (nginx-proxy-manager) o el navegador pueden preferir HTML, este es el
 * comportamiento por defecto que rompe el frontend.</p>
 */
@RestControllerAdvice
public class GlobalRestErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestErrorHandler.class);

    /**
     * Captura cuando el archivo subido excede
     * {@code spring.servlet.multipart.max-file-size} o
     * {@code spring.servlet.multipart.max-request-size}.
     *
     * <p>Spring Boot 3.x: {@code MaxUploadSizeExceededException} se lanza desde
     * el resolver de multipart cuando se supera el límite.</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUpload(MaxUploadSizeExceededException ex) {
        long maxMb = ex.getMaxUploadSize() > 0 ? ex.getMaxUploadSize() / (1024L * 1024L) : -1L;
        log.warn("Upload rechazado: excede el límite de {} MB", maxMb);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of(
                        "mensaje", "El archivo excede el tamaño máximo permitido" +
                                   (maxMb > 0 ? " (" + maxMb + " MB)." : "."),
                        "tipo", "PAYLOAD_TOO_LARGE",
                        "maxFileSizeMb", maxMb
                ));
    }

    /**
     * Fallback para errores 413 que vienen de nginx-proxy-manager (que corta la
     * conexión antes de llegar a Spring). Esto nunca debería invocarse en
     * operación normal porque la excepción se devuelve como HTML — pero lo
     * dejamos documentado para cualquier caso futuro.
     */
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<?> handleMultipartGeneric(org.springframework.web.multipart.MultipartException ex) {
        log.warn("Multipart error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "mensaje", "No se pudo procesar el archivo subido.",
                        "tipo", "MULTIPART_ERROR",
                        "detalle", ex.getMessage() != null ? ex.getMessage() : ""
                ));
    }

    /**
     * Captura cualquier {@link VirusTotalException} que el controller no haya
     * manejado explícitamente (caso fail-closed o archivo inválido en VT).
     * Devuelve 400 con detalle para que el frontend muestre el mensaje al admin.
     */
    @ExceptionHandler(VirusTotalException.class)
    public ResponseEntity<?> handleVirusTotal(VirusTotalException ex) {
        log.warn("VirusTotal rechazó la operación ({}): {}", ex.getReason(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "mensaje", ex.getMessage(),
                        "tipo", "VIRUS_TOTAL_" + ex.getReason().name(),
                        "reason", ex.getReason().name()
                ));
    }
}

package org.reciclame.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Resultado del escaneo antivirus de un archivo vía VirusTotal.
 *
 * <p>Se persiste en {@code normativas.vt_*} / {@code estudios.vt_*} y se cachea
 * en Redis para evitar pegarle a VT por cada subida del mismo SHA-256.</p>
 *
 * <p><b>Veredictos posibles:</b></p>
 * <ul>
 *   <li>{@code CLEAN} — VT no detectó malware en ninguno de los motores.</li>
 *   <li>{@code MALICIOUS} — al menos un motor marcó el archivo como malicioso.</li>
 *   <li>{@code SUSPICIOUS} — algunos motores marcaron como sospechoso (heurística).</li>
 *   <li>{@code UNDETECTED} — VT no tiene veredicto final todavía (poll agotado).</li>
 *   <li>{@code SKIPPED} — no se pudo escanear (fail-open o archivo no soportado).</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VirusTotalVerdict {

    /** SHA-256 del contenido del archivo (clave de caché). */
    private String sha256;

    /** Veredicto final mapeado a un enum legible. */
    private Verdict verdict;

    /** Cantidad de motores que marcaron el archivo como malicioso. */
    private Integer maliciousEngines;

    /** Cantidad de motores que lo marcaron como sospechoso. */
    private Integer suspiciousEngines;

    /** Total de motores que participaron en el análisis. */
    private Integer totalEngines;

    /** Cuándo se hizo el análisis contra VT (UTC). */
    private Instant scannedAt;

    /** true si no se pudo contactar a VT (cuota, red, timeout) y se aplicó fail-open. */
    @Builder.Default
    private boolean skipped = false;

    /** Mensaje humano (ej. "VirusTotal no respondió — fail-open aplicado"). */
    private String message;

    public enum Verdict {
        CLEAN,
        MALICIOUS,
        SUSPICIOUS,
        UNDETECTED,
        SKIPPED
    }

    /** Helper: ¿se permite subir el archivo a MinIO? */
    public boolean isAllowed() {
        return verdict == Verdict.CLEAN || verdict == Verdict.UNDETECTED || verdict == Verdict.SKIPPED;
    }
}
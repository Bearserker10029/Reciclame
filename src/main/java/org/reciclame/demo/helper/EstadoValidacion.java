package org.reciclame.demo.helper;

/**
 * Estado de validación del archivo adjunto, derivado del par {@code (archivo_url, vt_verdict)}
 * sin necesidad de columnas adicionales en la BD.
 *
 * <p>Reglas de inferencia:</p>
 * <ul>
 *   <li>{@link #PENDIENTE}: archivo_url empieza con {@code staging://} (acaba de subirse al
 *       bucket staging y aún no pasó por VirusTotal).</li>
 *   <li>{@link #RECHAZADO}: vt_verdict = {@code MALICIOUS}. El archivo queda en staging
 *       y no se promueve al bucket definitivo.</li>
 *   <li>{@link #APROBADO}: vt_verdict presente y distinto de MALICIOUS (CLEAN, UNDETECTED,
 *       SUSPICIOUS o SKIPPED). El archivo ya vive en el bucket definitivo.</li>
 *   <li>{@code null}: no hay archivo adjunto (archivo_url vacío o nulo).</li>
 * </ul>
 */
public enum EstadoValidacion {
    PENDIENTE,
    APROBADO,
    RECHAZADO;

    /**
     * Infiere el estado de validación a partir de la URL del archivo y el veredicto VT.
     * Si la URL no apunta a staging y el veredicto es nulo, devuelve {@code null}
     * (no hay archivo que validar).
     */
    public static EstadoValidacion inferir(String archivoUrl, String vtVerdict) {
        if (archivoUrl == null || archivoUrl.isBlank()) {
            return null;
        }
        if (archivoUrl.startsWith("staging://")) {
            return PENDIENTE;
        }
        if ("MALICIOUS".equals(vtVerdict)) {
            return RECHAZADO;
        }
        if (vtVerdict != null) {
            return APROBADO;
        }
        return null;
    }

    /**
     * Construye el marcador de staging que se persiste en {@code archivo_url} mientras
     * el archivo vive en el bucket reciclame-staging y aún no fue promovido.
     * El marker embebe el nombre completo del objeto en staging (incluye nombre de archivo
     * saneado + extensión) para que las operaciones get/delete no necesiten volver a
     * derivar el nombre.
     */
    public static String stagingMarker(String stagingObjectName) {
        return "staging://" + stagingObjectName;
    }

    /**
     * Extrae el nombre del objeto en staging del marker. Devuelve {@code null} si la URL
     * no es un marker válido.
     */
    public static String extractStagingObjectName(String archivoUrl) {
        if (archivoUrl == null || !archivoUrl.startsWith("staging://")) return null;
        return archivoUrl.substring("staging://".length());
    }
}

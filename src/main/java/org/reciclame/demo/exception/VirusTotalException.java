package org.reciclame.demo.exception;

/**
 * Excepción lanzada cuando VirusTotal rechaza un archivo (malware detectado) o
 * cuando la operación de escaneo falla y la política es fail-closed.
 *
 * <p>Se traduce a HTTP 400 en {@link org.reciclame.demo.config.GlobalRestErrorHandler}
 * para que el frontend muestre el mensaje al usuario.</p>
 */
public class VirusTotalException extends RuntimeException {

    public enum Reason {
        MALWARE_DETECTED,
        VT_UNAVAILABLE_FAIL_CLOSED,
        VT_QUOTA_EXCEEDED,
        VT_INVALID_FILE,
        VT_POLL_TIMEOUT
    }

    private final Reason reason;

    public VirusTotalException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public VirusTotalException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
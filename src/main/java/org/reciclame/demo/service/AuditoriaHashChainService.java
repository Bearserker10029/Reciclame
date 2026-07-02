package org.reciclame.demo.service;

import org.reciclame.demo.model.Auditoria;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Implementación del hash chain para la tabla {@code auditoria}.
 *
 * <h2>Algoritmo</h2>
 * <ul>
 *   <li>Función: SHA-256 (provista por {@link MessageDigest} de la JVM).</li>
 *   <li>Encoding: hex en minúsculas, 64 caracteres.</li>
 *   <li>Génesis (primer registro): {@code SHA-256("RECICLAME_HASH_CHAIN_GENESIS_v1")}.</li>
 *   <li>Bloque N: {@code SHA-256(hashAnterior || "|" || id || "|" ||
 *       fechaAccion_ISO || "|" || accion || "|" || modulo || "|" ||
 *       usuarioId || "|" || descripcion || "|" || estado)}.</li>
 * </ul>
 *
 * <h2>Por qué sirve para presentación</h2>
 * Cualquier edición posterior a la inserción de una fila — sea por SQL
 * directo, por un atacante con permisos de admin, o por un bug en el
 * código — invalidará TODOS los hashes siguientes. La verificación
 * detecta la primera fila manipulada, su id y la fila "esperada".
 *
 * <h2>Propiedades</h2>
 * <ul>
 *   <li><b>Puro</b>: no toca BD ni tiene efectos colaterales — recibe
 *       el bloque actual + opcionalmente el {@code hashAnterior} y
 *       devuelve el {@code hashActual}. Eso lo hace trivial de probar.</li>
 *   <li><b>Determinista</b>: para los mismos inputs produce siempre el
 *       mismo output (excepto el timestamp ISO, que es fijo una vez
 *       persistido).</li>
 *   <li><b>Tamper-evident</b>: cubre todos los campos del registro.</li>
 * </ul>
 */
@Service
public class AuditoriaHashChainService {

    /** Salt del bloque génesis. Versión incluido para invalidar cadenas viejas. */
    public static final String GENESIS_SALT = "RECICLAME_HASH_CHAIN_GENESIS_v1";

    /** Separador interno del payload — no puede aparecer en ninguno de los campos. */
    private static final String FIELD_SEPARATOR = "|";

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    /**
     * Calcula el hash del bloque génesis (primer registro de la cadena).
     */
    public String calcularHashGenesis() {
        return sha256Hex(GENESIS_SALT);
    }

    /**
     * Calcula el hash actual del registro a partir de su hash anterior
     * (o {@code null} para el génesis) y los campos cubiertos.
     *
     * @param anterior      hash del registro previo, o {@code null}
     *                      si este es el primer registro histórico.
     * @param a             registro que se está firmando.
     * @return              hash SHA-256 hex en minúsculas (64 chars).
     */
    public String calcularHashActual(String anterior, Auditoria a) {
        Objects.requireNonNull(a, "auditoria");
        String prev = anterior != null ? anterior : calcularHashGenesis();

        String payload = String.join(FIELD_SEPARATOR,
                prev,
                String.valueOf(a.getId()),
                a.getFechaAccion() != null
                        ? ISO_FORMATTER.format(a.getFechaAccion())
                        : Instant.EPOCH.toString(),
                nullSafe(a.getAccion()),
                nullSafe(a.getModulo()),
                a.getUsuarioId() != null
                        ? String.valueOf(a.getUsuarioId()) : "",
                nullSafe(a.getDescripcion()),
                nullSafe(a.getEstado()));

        return sha256Hex(payload);
    }

    /**
     * Verifica una cadena completa. Devuelve el id del primer registro
     * que NO cumpla, junto con el hash esperado y el encontrado.
     *
     * <h3>Tratamiento de filas previas al despliegue</h3>
     * Las filas con {@code hashActual == null} son consideradas "anteriores
     * al despliegue" del hash chain. NO se verifican (sus campos
     * originales quedan intactos) pero se cuentan para que el resultado
     * sea transparente. La cadena firmada que continúa después de ellas
     * se valida normalmente, empezando por el bloque génesis si esas
     * filas previas son las últimas de la tabla, o encadenándose al
     * último bloque firmado si las hubiera.
     *
     * @param orderedAsc    lista de TODOS los registros en orden
     *                      ascendente por id (de más antiguo a más nuevo).
     * @return              resultado con estado, ids, hashes y conteos.
     */
    public ResultadoVerificacion verificar(List<Auditoria> orderedAsc) {
        if (orderedAsc == null || orderedAsc.isEmpty()) {
            return ResultadoVerificacion.vacio();
        }

        int total = orderedAsc.size();
        int verificados = 0;
        int sinFirma = 0;
        // El "anterior esperado" para el primer bloque firmado será el
        // génesis. Si después del bloque génesis hay filas sin firma
        // (registros previos al despliegue), las saltamos y seguimos
        // esperando el génesis para el siguiente bloque firmado.
        String esperadoAnterior = calcularHashGenesis();

        for (Auditoria actual : orderedAsc) {
            // Caso: fila previa al despliegue (sin firma). La saltamos
            // y dejamos el "anterior esperado" como está (sigue siendo
            // el génesis si no hemos visto ninguna firmada todavía).
            if (actual.getHashActual() == null) {
                sinFirma++;
                continue;
            }
            // Si tiene hashActual pero no hashAnterior, es el primer
            // bloque firmado — su anterior esperado debe ser el génesis.
            if (actual.getHashAnterior() == null) {
                // Si esperadoAnterior NO es el génesis en este punto,
                // es un estado inválido: el bloque no está enlazado.
                if (!Objects.equals(esperadoAnterior, calcularHashGenesis())) {
                    return ResultadoVerificacion.fallo(total, verificados,
                            actual.getId(),
                            esperadoAnterior,
                            actual.getHashAnterior(),
                            actual.getHashActual(),
                            "Primer bloque firmado detectado pero el anterior esperado no es el génesis "
                          + "(estado inconsistente — posiblemente se borró un bloque firmado).");
                }
                // Si por el contrario el anterior esperado SÍ es el
                // génesis, ya está alineado — seguimos.
            } else if (!Objects.equals(actual.getHashAnterior(), esperadoAnterior)) {
                return ResultadoVerificacion.fallo(total, verificados,
                        actual.getId(),
                        esperadoAnterior,
                        actual.getHashAnterior(),
                        actual.getHashActual(),
                        "Encadenamiento roto: hashAnterior no coincide con el hashActual del bloque previo firmado.");
            }

            // Validar contenido: recalcular y comparar.
            String recalculado = calcularHashActual(
                    actual.getHashAnterior() != null
                            ? actual.getHashAnterior()
                            : calcularHashGenesis(),
                    actual);
            if (!Objects.equals(recalculado, actual.getHashActual())) {
                return ResultadoVerificacion.fallo(total, verificados,
                        actual.getId(),
                        recalculado,
                        actual.getHashAnterior(),
                        actual.getHashActual(),
                        "Contenido alterado: el hashActual no se corresponde con los campos de la fila.");
            }

            verificados++;
            esperadoAnterior = actual.getHashActual();
        }

        ResultadoVerificacion base = ResultadoVerificacion.integro(total, verificados);
        return base.conContextoSinFirma(sinFirma);
    }

    // ── Utils ──────────────────────────────────────────────────────────

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está garantizado en toda JVM.
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    // ── DTO de resultado ──────────────────────────────────────────────

    /**
     * Resultado de {@link #verificar(List)}. Inmutable, serializable a JSON
     * para que el endpoint REST lo devuelva directo.
     */
    public static final class ResultadoVerificacion {
        private final boolean integro;
        private final int totalRegistros;
        private final int registrosVerificados;
        private final int registrosSinFirma;
        private final Long idFalla;
        private final String hashEsperado;
        private final String hashAnteriorEncontrado;
        private final String hashActualEncontrado;
        private final String mensaje;

        private ResultadoVerificacion(boolean integro, int total, int verificados,
                                      int sinFirma, Long idFalla, String hashEsperado,
                                      String hashAnterior, String hashActual,
                                      String mensaje) {
            this.integro = integro;
            this.totalRegistros = total;
            this.registrosVerificados = verificados;
            this.registrosSinFirma = sinFirma;
            this.idFalla = idFalla;
            this.hashEsperado = hashEsperado;
            this.hashAnteriorEncontrado = hashAnterior;
            this.hashActualEncontrado = hashActual;
            this.mensaje = mensaje;
        }

        static ResultadoVerificacion vacio() {
            return new ResultadoVerificacion(true, 0, 0, 0, null, null, null, null,
                    "La tabla de auditoría está vacía.");
        }

        static ResultadoVerificacion integro(int total, int verificados) {
            return new ResultadoVerificacion(true, total, verificados, 0, null, null, null, null,
                    "Cadena íntegra: todos los registros verificados.");
        }

        ResultadoVerificacion conContextoSinFirma(int sinFirma) {
            if (sinFirma == 0) return this;
            String extra = (this.mensaje == null ? "" : this.mensaje + " ")
                    + "(" + sinFirma + " fila(s) previa(s) al despliegue del hash chain, "
                    + "no se verifican pero quedan intactas).";
            return new ResultadoVerificacion(this.integro,
                    this.totalRegistros, this.registrosVerificados, sinFirma,
                    this.idFalla, this.hashEsperado,
                    this.hashAnteriorEncontrado, this.hashActualEncontrado,
                    extra.trim());
        }

        static ResultadoVerificacion fallo(int total, int verificados,
                                           Long idFalla, String hashEsperado,
                                           String hashAnterior, String hashActual,
                                           String mensaje) {
            return new ResultadoVerificacion(false, total, verificados, 0, idFalla,
                    hashEsperado, hashAnterior, hashActual, mensaje);
        }

        public boolean isIntegro() { return integro; }
        public int getTotalRegistros() { return totalRegistros; }
        public int getRegistrosVerificados() { return registrosVerificados; }
        public int getRegistrosSinFirma() { return registrosSinFirma; }
        public Long getIdFalla() { return idFalla; }
        public String getHashEsperado() { return hashEsperado; }
        public String getHashAnteriorEncontrado() { return hashAnteriorEncontrado; }
        public String getHashActualEncontrado() { return hashActualEncontrado; }
        public String getMensaje() { return mensaje; }
    }
}
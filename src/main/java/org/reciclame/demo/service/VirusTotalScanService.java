package org.reciclame.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.reciclame.demo.dto.VirusTotalVerdict;
import org.reciclame.demo.dto.VirusTotalVerdict.Verdict;
import org.reciclame.demo.exception.VirusTotalException;
import org.reciclame.demo.exception.VirusTotalException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * Servicio que escanea archivos subidos contra la API pública de VirusTotal.
 *
 * <p><b>Flujo:</b></p>
 * <ol>
 *   <li>Calcula SHA-256 del archivo (clave natural del escaneo).</li>
 *   <li>Busca el veredicto en Redis ({@link VirusTotalScanCacheService}). Si existe,
 *       lo devuelve inmediatamente (sin pegarle a VT).</li>
 *   <li>Consulta {@code GET /api/v3/files/{sha256}}. Si VT ya lo conoce, parsea
 *       {@code last_analysis_stats} y devuelve el veredicto.</li>
 *   <li>Si VT no lo conoce, sube el archivo vía {@code POST /api/v3/files} (multipart).
 *       VT devuelve un {@code analysis_id}.</li>
 *   <li>Hace polling a {@code GET /api/v3/analyses/{analysis_id}} hasta que el
 *       estado sea "completed" o se agote el número de intentos.</li>
 *   <li>Si el análisis no completa a tiempo, devuelve veredicto
 *       {@link Verdict#SKIPPED} y se aplica la política {@code virustotal.fail-open}.</li>
 * </ol>
 *
 * <p><b>Rate limits públicos:</b> 4 requests / minuto / IP, 500 / día.
 * Por eso la caché en Redis es clave.</p>
 */
@Service
public class VirusTotalScanService {

    private static final Logger log = LoggerFactory.getLogger(VirusTotalScanService.class);

    @Value("${virustotal.api.key:}")
    private String apiKey;

    @Value("${virustotal.api.url:https://www.virustotal.com/api/v3}")
    private String apiUrl;

    @Value("${virustotal.poll.max-attempts:12}")
    private int maxPollAttempts;

    @Value("${virustotal.poll.interval-ms:5000}")
    private long pollIntervalMs;

    @Value("${virustotal.fail-open:true}")
    private boolean failOpen;

    @Value("${virustotal.timeout-ms:30000}")
    private long timeoutMs;

    private final VirusTotalScanCacheService cache;
    private final ObjectMapper objectMapper;
    private OkHttpClient httpClient;

    public VirusTotalScanService(VirusTotalScanCacheService cache) {
        this.cache = cache;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // para serializar Instant en logs/errores
    }

    @PostConstruct
    void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * Escanea el archivo y devuelve el veredicto. Nunca devuelve {@code null}.
     * Si VT no responde y la política es fail-open, devuelve un veredicto
     * {@link Verdict#SKIPPED} con mensaje explicativo.
     */
    public VirusTotalVerdict scan(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return VirusTotalVerdict.builder()
                    .verdict(Verdict.SKIPPED)
                    .skipped(true)
                    .message("Archivo vacío: no se escaneó")
                    .scannedAt(Instant.now())
                    .build();
        }
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("tu_api_key_aqui")) {
            log.warn("VIRUSTOTAL_API_KEY no configurada: aplicando fail-open (veredicto=SKIPPED).");
            return VirusTotalVerdict.builder()
                    .verdict(Verdict.SKIPPED)
                    .skipped(true)
                    .message("VirusTotal no configurado: el archivo se subió sin escaneo")
                    .scannedAt(Instant.now())
                    .build();
        }

        String sha256;
        try {
            sha256 = sha256(archivo);
            log.info("VT scan: archivo='{}' size={}B sha256={}",
                archivo.getOriginalFilename(), archivo.getSize(), sha256);
        } catch (IOException e) {
            log.error("No se pudo calcular SHA-256 del archivo: {}", e.getMessage());
            return failOpenVerdict("No se pudo calcular SHA-256: " + e.getMessage());
        }

        // 1) cache
        var cached = cache.get(sha256);
        if (cached.isPresent()) {
            VirusTotalVerdict v = cached.get();
            // NO confiar en cache de fallos transitorios (cuota, red, key inválida).
            // Si el veredicto guardado es SKIPPED por motivos recuperables,
            // dejamos que VT lo intente de nuevo.
            if (v.getVerdict() == Verdict.SKIPPED && isRecoverableSkipMessage(v.getMessage())) {
                log.warn("VirusTotal cache SKIP recuperable sha={}, reintentando contra VT", sha256);
                cache.evict(sha256);
            } else {
                log.debug("VirusTotal cache HIT sha={} verdict={}", sha256, v.getVerdict());
                return v;
            }
        }

        // 2) consulta VT por hash
        try {
            VirusTotalVerdict fromHash = lookupByHash(sha256);
            if (fromHash != null) {
                log.info("VT lookup HIT sha={} verdict={} malicious={} suspicious={} total={}",
                    sha256, fromHash.getVerdict(), fromHash.getMaliciousEngines(),
                    fromHash.getSuspiciousEngines(), fromHash.getTotalEngines());
                cache.put(fromHash);
                return fromHash;
            }
            log.info("VT lookup MISS sha={} (no conocido por VT), subiendo archivo", sha256);
        } catch (VirusTotalException e) {
            if (e.getReason() == Reason.VT_QUOTA_EXCEEDED) {
                return failOpenVerdict("Cuota de VirusTotal agotada: " + e.getMessage());
            }
            if (e.getReason() == Reason.VT_INVALID_FILE) {
                throw e; // 400 al cliente
            }
            // 5xx o red → fail-open
            return failOpenVerdict("VirusTotal no respondió (lookup): " + e.getMessage());
        } catch (Exception e) {
            return failOpenVerdict("Error consultando VirusTotal: " + e.getMessage());
        }

        // 3) no lo conoce → subir archivo y esperar análisis
        String analysisId;
        try {
            analysisId = uploadForAnalysis(archivo);
        } catch (VirusTotalException e) {
            if (e.getReason() == Reason.VT_QUOTA_EXCEEDED) {
                return failOpenVerdict("Cuota de VirusTotal agotada: " + e.getMessage());
            }
            if (e.getReason() == Reason.VT_INVALID_FILE) {
                throw e;
            }
            return failOpenVerdict("VirusTotal no respondió (upload): " + e.getMessage());
        } catch (Exception e) {
            return failOpenVerdict("Error subiendo archivo a VirusTotal: " + e.getMessage());
        }

        // 4) poll
        VirusTotalVerdict polled = pollAnalysis(analysisId);
        cache.put(polled);
        return polled;
    }

    // ── Helpers ──────────────────────────────────────────────

    /**
     * Determina si un veredicto SKIPPED cacheado debe reintentarse.
     * SKIPs por archivo vacío, key no configurada o archivo inválido NO se reintentan.
     * SKIPs por cuota, red, 5xx o key inválida SÍ se reintentan (transitorios).
     */
    private boolean isRecoverableSkipMessage(String msg) {
        if (msg == null) return true; // sin mensaje = probablemente skip técnico → reintentar
        String m = msg.toLowerCase();
        return m.contains("cuota") || m.contains("429")
            || m.contains("no respondió") || m.contains("error i/o")
            || m.contains("error consultando") || m.contains("error subiendo")
            || m.contains("vt_unavailable") || m.contains("http 5");
    }

    private VirusTotalVerdict failOpenVerdict(String message) {
        if (!failOpen) {
            throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED, message);
        }
        log.warn("VirusTotal fail-open: {}", message);
        return VirusTotalVerdict.builder()
                .verdict(Verdict.SKIPPED)
                .skipped(true)
                .message(message)
                .scannedAt(Instant.now())
                .build();
    }

    private String sha256(MultipartFile archivo) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (var in = archivo.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 no disponible en el JDK", e);
        }
    }

    /**
     * GET /files/{sha256}. Devuelve el veredicto si VT lo tiene; null si no lo conoce.
     * Lanza VirusTotalException con el reason adecuado si la API devuelve 4xx/5xx.
     */
    private VirusTotalVerdict lookupByHash(String sha256) {
        Request req = baseRequest("/files/" + sha256).get().build();
        try (Response res = httpClient.newCall(req).execute()) {
            if (res.code() == 404) {
                log.debug("VirusTotal no tiene el archivo sha={}, se subirá", sha256);
                return null;
            }
            if (res.code() == 401) {
                // API key inválida o no autorizada. Es recuperable (puede rotarse la key),
                // NO guardamos cache y devolvemos fail-open.
                throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                        "VT 401 Unauthorized — API key inválida o sin permisos");
            }
            if (res.code() == 429) {
                throw new VirusTotalException(Reason.VT_QUOTA_EXCEEDED,
                        "VT 429 Too Many Requests (límite 4 req/min)");
            }
            if (res.code() == 400 || res.code() == 403) {
                throw new VirusTotalException(Reason.VT_INVALID_FILE,
                        "VT " + res.code() + ": " + safeBody(res));
            }
            if (!res.isSuccessful()) {
                throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                        "VT HTTP " + res.code() + ": " + safeBody(res));
            }
            String body = res.body().string();
            return parseFileReport(sha256, body);
        } catch (VirusTotalException e) {
            throw e;
        } catch (IOException e) {
            throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                    "Error I/O VT lookup: " + e.getMessage(), e);
        }
    }

    /**
     * POST /files con el archivo multipart. Devuelve el analysis_id.
     */
    private String uploadForAnalysis(MultipartFile archivo) {
        String filename = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename() : "archivo";
        MediaType mediaType = archivo.getContentType() != null
                ? MediaType.parse(archivo.getContentType())
                : MediaType.parse("application/octet-stream");

        try {
            RequestBody fileBody = RequestBody.create(archivo.getBytes(), mediaType);
            RequestBody multipart = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, fileBody)
                    .build();

            Request req = baseRequest("/files").post(multipart).build();
            try (Response res = httpClient.newCall(req).execute()) {
                if (res.code() == 429) {
                    throw new VirusTotalException(Reason.VT_QUOTA_EXCEEDED,
                            "VT 429 Too Many Requests en upload");
                }
                if (res.code() == 400 || res.code() == 403) {
                    throw new VirusTotalException(Reason.VT_INVALID_FILE,
                            "VT " + res.code() + " al subir: " + safeBody(res));
                }
                if (!res.isSuccessful()) {
                    throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                            "VT HTTP " + res.code() + " al subir: " + safeBody(res));
                }
                String body = res.body().string();
                JsonNode root = objectMapper.readTree(body);
                JsonNode id = root.path("data").path("id");
                if (id.isMissingNode() || !id.isTextual()) {
                    throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                            "VT no devolvió analysis_id en upload: " + body);
                }
                return id.asText();
            }
        } catch (VirusTotalException e) {
            throw e;
        } catch (IOException e) {
            throw new VirusTotalException(Reason.VT_UNAVAILABLE_FAIL_CLOSED,
                    "Error I/O subiendo a VT: " + e.getMessage(), e);
        }
    }

    /**
     * Poll GET /analyses/{id}. Devuelve SKIPPED si se agotaron los intentos.
     */
    private VirusTotalVerdict pollAnalysis(String analysisId) {
        for (int i = 1; i <= maxPollAttempts; i++) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            Request req = baseRequest("/analyses/" + analysisId).get().build();
            try (Response res = httpClient.newCall(req).execute()) {
                if (res.code() == 429) {
                    // cuota: no seguir gastando
                    return VirusTotalVerdict.builder()
                            .verdict(Verdict.SKIPPED).skipped(true)
                            .message("Cuota VT agotada durante el poll")
                            .scannedAt(Instant.now())
                            .build();
                }
                if (!res.isSuccessful()) {
                    log.warn("Poll VT intento {}/{} → HTTP {}", i, maxPollAttempts, res.code());
                    continue;
                }
                String body = res.body().string();
                JsonNode root = objectMapper.readTree(body);
                String status = root.path("data").path("attributes").path("status").asText("");
                if ("completed".equalsIgnoreCase(status)) {
                    JsonNode stats = root.path("data").path("attributes")
                            .path("stats");
                    return buildVerdictFromStats(stats, Instant.now(), null);
                }
                log.debug("Poll VT intento {}/{} → status={}", i, maxPollAttempts, status);
            } catch (IOException e) {
                log.warn("Poll VT intento {}/{} → I/O error: {}", i, maxPollAttempts, e.getMessage());
            }
        }
        return VirusTotalVerdict.builder()
                .verdict(Verdict.UNDETECTED).skipped(true)
                .message("VirusTotal no completó el análisis a tiempo (" + maxPollAttempts + " intentos)")
                .scannedAt(Instant.now())
                .build();
    }

    private VirusTotalVerdict parseFileReport(String sha256, String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode stats = root.path("data").path("attributes").path("last_analysis_stats");
        return buildVerdictFromStats(stats, Instant.now(), sha256);
    }

    private VirusTotalVerdict buildVerdictFromStats(JsonNode stats, Instant when, String sha256) {
        int malicious = stats.path("malicious").asInt(0);
        int suspicious = stats.path("suspicious").asInt(0);
        int undetected = stats.path("undetected").asInt(0);
        int harmless = stats.path("harmless").asInt(0);
        int timeout = stats.path("timeout").asInt(0);
        int typeUnsupported = stats.path("type-unsupported").asInt(0);
        int failure = stats.path("failure").asInt(0);
        int confirmedTimeout = stats.path("confirmed-timeout").asInt(0);
        int total = malicious + suspicious + undetected + harmless + timeout
                + typeUnsupported + failure + confirmedTimeout;

        Verdict verdict;
        if (malicious > 0) verdict = Verdict.MALICIOUS;
        else if (suspicious > 0) verdict = Verdict.SUSPICIOUS;
        else verdict = Verdict.CLEAN;

        String message = null;
        if (verdict == Verdict.MALICIOUS) {
            message = malicious + " motores marcaron el archivo como malicioso";
        } else if (verdict == Verdict.SUSPICIOUS) {
            message = suspicious + " motores marcaron el archivo como sospechoso";
        }

        return VirusTotalVerdict.builder()
                .sha256(sha256)
                .verdict(verdict)
                .maliciousEngines(malicious)
                .suspiciousEngines(suspicious)
                .totalEngines(total)
                .scannedAt(when)
                .message(message)
                .build();
    }

    private Request.Builder baseRequest(String path) {
        return new Request.Builder()
                .url(apiUrl + path)
                .header("x-apikey", apiKey)
                .header("Accept", "application/json");
    }

    private String safeBody(Response res) {
        try {
            return res.body() != null ? res.body().string() : "";
        } catch (IOException e) {
            return "<body no legible>";
        }
    }
}
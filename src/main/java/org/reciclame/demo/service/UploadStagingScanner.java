package org.reciclame.demo.service;

import org.reciclame.demo.dto.VirusTotalVerdict;
import org.reciclame.demo.helper.EstadoValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

/**
 * Worker async que ejecuta la segunda fase del flujo de subida:
 * <ol>
 *   <li>Baja el archivo del bucket staging de MinIO a un archivo temporal local.</li>
 *   <li>Lanza el escaneo contra VirusTotal (cache Redis primero → si HIT, retorno inmediato).</li>
 *   <li>Si el veredicto es ALLOWED, promueve el archivo al bucket definitivo (copy + delete).</li>
 *   <li>Si es MALICIOUS, deja el archivo en staging (limpieza TTL lo borra) y marca el registro.</li>
 *   <li>Actualiza el registro Normativa/Estudio con los datos del veredicto.</li>
 * </ol>
 *
 * <p>Se ejecuta dentro del pool {@code stagingScannerExecutor} definido en
 * {@link org.reciclame.demo.config.AsyncConfig}.</p>
 */
@Service
public class UploadStagingScanner {

    private static final Logger log = LoggerFactory.getLogger(UploadStagingScanner.class);

    private final UploadStagingService stagingService;
    private final VirusTotalScanService virusTotalScanService;
    private final MinioStorageService minioStorageService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;

    /** Tamaño máximo (bytes) a bajar a disco local antes de scan. > esto usa streaming directo. */
    @Value("${upload.scanner.max-local-size-bytes:52428800}")
    private long maxLocalSizeBytes;

    public UploadStagingScanner(UploadStagingService stagingService,
                                VirusTotalScanService virusTotalScanService,
                                MinioStorageService minioStorageService,
                                NormativaService normativaService,
                                EstudioService estudioService) {
        this.stagingService = stagingService;
        this.virusTotalScanService = virusTotalScanService;
        this.minioStorageService = minioStorageService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
    }

    /**
     * Encola una validación en el pool async. El retorno es inmediato; el trabajo
     * pesado (download + scan + promote) ocurre en otro hilo.
     *
     * @param tipo            {@code "NORMATIVA"} o {@code "ESTUDIO"}.
     * @param refId           ID del registro en BD.
     * @param archivoStagingUrl  URL con marker {@code staging://<objectName>}.
     * @param archivoTipoMime MIME del archivo (propagado al bucket docs al promover).
     * @param correoAutor     Correo del autor (solo para logs).
     */
    @Async("stagingScannerExecutor")
    public void procesarAsync(String tipo, Long refId, String archivoStagingUrl,
                              String archivoTipoMime, String correoAutor) {
        log.info("Scanner async iniciando: tipo={} refId={} staging={} autor={}",
                tipo, refId, archivoStagingUrl, correoAutor);
        String stagingObject = EstadoValidacion.extractStagingObjectName(archivoStagingUrl);
        if (stagingObject == null) {
            log.error("archivoStagingUrl inválido para escaneo: {}", archivoStagingUrl);
            return;
        }
        procesar(tipo, refId, stagingObject, archivoTipoMime, correoAutor);
    }

    /**
     * Versión bloqueante (usada por el @Scheduled de recuperación).
     */
    public void procesar(String tipo, Long refId, String stagingObject, String archivoTipoMime, String correoAutor) {
        Instant started = Instant.now();
        VirusTotalVerdict verdict = null;
        String urlFinal = archivoStagingUrlFor(tipo, stagingObject);
        try {
            // 1) Bajar a archivo local para que el scanner pueda leer del path.
            File tmpFile = downloadStagingToTmp(stagingObject, archivoTipoMime);
            try {
                // 2) Escanear contra VT.
                verdict = virusTotalScanService.scan(tmpFile, stagingObject, archivoTipoMime);
                log.info("VT verdict para tipo={} refId={}: verdict={} malicious={} suspicious={}",
                        tipo, refId, verdict.getVerdict(),
                        verdict.getMaliciousEngines(), verdict.getSuspiciousEngines());

                // 3) Si allowed → promover a docs.
                if (verdict.isAllowed()) {
                    String promoteResult = stagingService.promoteToDocs(stagingObject, archivoTipoMime);
                    urlFinal = promoteResult;
                    log.info("Archivo promovido a docs: tipo={} refId={} url={}", tipo, refId, promoteResult);
                } else {
                    log.warn("Archivo NO promovido (MALICIOUS u otro rechazo), queda en staging: tipo={} refId={}",
                            tipo, refId);
                }
            } finally {
                if (tmpFile != null && tmpFile.exists()) {
                    try { Files.deleteIfExists(tmpFile.toPath()); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ex) {
            log.error("Error escaneando staging tipo={} refId={} obj={}: {}",
                    tipo, refId, stagingObject, ex.getMessage(), ex);
        }

        // 4) Actualizar BD con el veredicto (o error). Si verdict es null por excepción,
        // usamos SKIPPED para que el estado inferido siga siendo PENDIENTE (no se promueve).
        if (verdict == null) {
            verdict = VirusTotalVerdict.builder()
                    .verdict(VirusTotalVerdict.Verdict.SKIPPED)
                    .skipped(true)
                    .message("Error interno del scanner: revise logs del servidor")
                    .scannedAt(Instant.now())
                    .build();
        }
        try {
            if ("NORMATIVA".equalsIgnoreCase(tipo)) {
                normativaService.actualizarEstadoValidacion(refId, verdict, urlFinal,
                        null, "Sistema", null, "Scanner-Async/1.0");
            } else if ("ESTUDIO".equalsIgnoreCase(tipo)) {
                estudioService.actualizarEstadoValidacion(refId, verdict, urlFinal,
                        null, "Sistema", null, "Scanner-Async/1.0");
            } else {
                log.error("Tipo desconocido en scanner async: {}", tipo);
            }
        } catch (Exception ex) {
            log.error("Error actualizando BD tras escaneo tipo={} refId={}: {}", tipo, refId, ex.getMessage(), ex);
        }
        log.info("Scanner async finalizado: tipo={} refId={} estado={} elapsedMs={}",
                tipo, refId, verdict.getVerdict(), java.time.Duration.between(started, Instant.now()).toMillis());
    }

    private String archivoStagingUrlFor(String tipo, String stagingObject) {
        return EstadoValidacion.stagingMarker(stagingObject);
    }

    /**
     * Baja el archivo del bucket staging a un {@link File} temporal local para que
     * {@code VirusTotalScanService.scan(File,...)} pueda leerlo como InputStream.
     * Usa un tmpdir configurable por JVM ({@code java.io.tmpdir}, default {@code /tmp}).
     */
    private File downloadStagingToTmp(String stagingObject, String contentType) throws java.io.IOException {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                "reciclame-scan-" + UUID.randomUUID() + "-" + stagingObject.replaceAll("[^a-zA-Z0-9._-]", "_"));
        try (InputStream in = stagingService.open(stagingObject)) {
            Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return tmp;
    }
}

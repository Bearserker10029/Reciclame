package org.reciclame.demo.scheduler;

import org.reciclame.demo.helper.EstadoValidacion;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.service.UploadStagingScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Red de seguridad para los escaneos antivirus que quedan colgados.
 *
 * <p>El escaneo de un archivo subido corre en un hilo async (ver
 * {@link UploadStagingScanner}); si ese hilo muere por una excepción, el pool
 * se satura y descarta la tarea, o el contenedor reinicia a mitad de escaneo,
 * el registro queda con {@code archivo_url = staging://...} para siempre
 * (estado PENDIENTE) y nadie lo reintenta. Este scheduler cierra ese hueco:
 * cada cierto tiempo busca los registros que siguen en staging desde hace más
 * del umbral y vuelve a lanzar su procesamiento de forma bloqueante.</p>
 *
 * <p>El umbral evita pisar escaneos que legítimamente están en curso (un
 * escaneo normal tarda a lo sumo ~1-2 min contando el polling a VirusTotal).</p>
 */
@Component
public class UploadRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(UploadRecoveryScheduler.class);

    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final UploadStagingScanner scanner;

    /** Minutos que un registro debe llevar en staging antes de considerarlo colgado. */
    @Value("${upload.recovery.stale-minutes:5}")
    private long staleMinutes;

    public UploadRecoveryScheduler(NormativaRepository normativaRepository,
                                   EstudioRepository estudioRepository,
                                   UploadStagingScanner scanner) {
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.scanner = scanner;
    }

    @Scheduled(fixedDelayString = "${upload.recovery.fixed-delay-ms:180000}")
    public void reprocesarColgados() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(staleMinutes));
        int recuperados = 0;

        for (Normativa n : normativaRepository.findByArchivoUrlStartingWith("staging://")) {
            if (esColgado(n.getUpdatedAt(), n.getCreatedAt(), limite)) {
                recuperados += reintentar("NORMATIVA", n.getId(), n.getArchivoUrl(), n.getArchivoTipoMime());
            }
        }
        for (Estudio e : estudioRepository.findByArchivoUrlStartingWith("staging://")) {
            if (esColgado(e.getUpdatedAt(), e.getCreatedAt(), limite)) {
                recuperados += reintentar("ESTUDIO", e.getId(), e.getArchivoUrl(), e.getArchivoTipoMime());
            }
        }

        if (recuperados > 0) {
            log.info("UploadRecoveryScheduler: {} escaneo(s) colgado(s) reprocesado(s).", recuperados);
        }
    }

    private boolean esColgado(Instant updatedAt, Instant createdAt, Instant limite) {
        Instant ref = updatedAt != null ? updatedAt : createdAt;
        // Sin fecha de referencia, tratarlo como colgado (mejor reintentar que dejarlo atascado).
        return ref == null || ref.isBefore(limite);
    }

    private int reintentar(String tipo, Long refId, String archivoUrl, String mime) {
        String stagingObject = EstadoValidacion.extractStagingObjectName(archivoUrl);
        if (stagingObject == null) {
            return 0;
        }
        try {
            log.warn("Reprocesando escaneo colgado: tipo={} refId={} staging={}", tipo, refId, stagingObject);
            // idAntiguoAEliminar = null: en recuperación ya no sabemos (ni importa) si
            // este upload reemplazaba a otro registro; el viejo se conserva intacto.
            scanner.procesar(tipo, refId, stagingObject, mime, null, null);
            return 1;
        } catch (Exception ex) {
            log.error("Fallo al reprocesar escaneo colgado tipo={} refId={}: {}", tipo, refId, ex.getMessage());
            return 0;
        }
    }
}

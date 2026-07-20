package org.reciclame.demo.scheduler;

import org.reciclame.demo.service.MinioPendingDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron que purga archivos de MinIO cuya fecha de gracia (72h por defecto) ya venció.
 * <p>
 * Corre cada hora. La latencia entre la fechaProgramada y la ejecución real es a lo sumo
 * 1h, lo cual es aceptable para un período de gracia de 72h.
 * </p>
 */
@Component
public class MinioCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(MinioCleanupScheduler.class);

    private final MinioPendingDeletionService pendingDeletionService;

    public MinioCleanupScheduler(MinioPendingDeletionService pendingDeletionService) {
        this.pendingDeletionService = pendingDeletionService;
    }

    /**
     * Ejecuta la purga de archivos vencidos. Configurable vía property
     * {@code minio.cleanup.cron} (formato cron) o {@code minio.cleanup.fixed-delay-ms}
     * (default: cada 1 hora, alineado al minuto 0).
     */
    @Scheduled(cron = "${minio.cleanup.cron:0 0 * * * *}")
    public void purgarArchivosVencidos() {
        long pendientesAntes = pendingDeletionService.countPendientes();
        int purgados = pendingDeletionService.purgarVencidos();
        long fallidos = pendingDeletionService.countFallidos();
        if (purgados > 0 || fallidos > 0) {
            log.info("MinIO cleanup: purgados={} pendientesAntes={} fallidosTotales={}",
                    purgados, pendientesAntes, fallidos);
        } else {
            log.debug("MinIO cleanup: nada que purgar ({} pendientes en cola)", pendientesAntes);
        }
    }
}

package org.reciclame.demo.service;

import org.reciclame.demo.model.MinioPendingDeletion;
import org.reciclame.demo.repository.MinioPendingDeletionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona la cola de archivos de MinIO pendientes de purga.
 * <p>
 * Reemplaza el borrado inmediato de archivos cuando se reemplaza o elimina un estudio/normativa.
 * En su lugar, programa la eliminación para dentro de N horas (default 72h),
 * dando margen al admin para recuperar un archivo reemplazado por error.
 * </p>
 * <p>
 * El borrado físico se hace en {@link #purgarVencidos()}, llamado periódicamente por
 * {@code MinioCleanupScheduler}.
 * </p>
 */
@Service
public class MinioPendingDeletionService {

    private static final Logger log = LoggerFactory.getLogger(MinioPendingDeletionService.class);

    private final MinioPendingDeletionRepository repository;
    private final MinioStorageService minioStorageService;

    /**
     * TTL configurable vía property. Default 72h según requerimiento del producto.
     */
    @Value("${minio.pending-deletion.ttl-hours:72}")
    private long ttlHours;

    /**
     * Máximo de reintentos si falla la purga física (ej. MinIO transitoriamente caído).
     * Pasado ese límite, la entrada queda en FALLIDO y requiere intervención manual.
     */
    @Value("${minio.pending-deletion.max-retries:3}")
    private int maxRetries;

    public MinioPendingDeletionService(MinioPendingDeletionRepository repository,
                                       MinioStorageService minioStorageService) {
        this.repository = repository;
        this.minioStorageService = minioStorageService;
    }

    /**
     * Programa la eliminación del archivo después de {@code ttlHours}.
     * <p>
     * Idempotente por objectName: si ya existe una entrada PENDIENTE para el mismo
     * objectName, no crea una nueva (típico cuando se hace doble click en "Eliminar"
     * o cuando se reemplaza el mismo archivo por el mismo archivo).
     * </p>
     *
     * @param archivoUrl     URL completa del archivo en MinIO (puede ser null si solo tenemos el objectName)
     * @param objectName     nombre del objeto en el bucket (key única)
     * @param documentoTipo  ESTUDIO o NORMATIVA
     * @param documentoId    id del documento (puede ser null si el doc ya fue borrado de BD)
     * @param motivo         REEMPLAZO o ELIMINACION
     */
    @Transactional
    public MinioPendingDeletion programarEliminacion(String archivoUrl,
                                                     String objectName,
                                                     String documentoTipo,
                                                     Long documentoId,
                                                     String motivo) {
        if (objectName == null || objectName.isBlank()) {
            log.debug("programarEliminacion: objectName vacío, nada que programar");
            return null;
        }

        // Si ya hay una entrada pendiente para el mismo objectName, no duplicar.
        Optional<MinioPendingDeletion> existente = repository
                .findFirstByObjectNameAndEstado(objectName, MinioPendingDeletion.ESTADO_PENDIENTE);
        if (existente.isPresent()) {
            log.info("Ya existe purga pendiente para objectName={}, no se duplica", objectName);
            return existente.get();
        }

        MinioPendingDeletion p = new MinioPendingDeletion();
        p.setObjectName(objectName);
        p.setArchivoUrl(archivoUrl);
        p.setDocumentoTipo(documentoTipo);
        p.setDocumentoId(documentoId);
        p.setMotivo(motivo);
        p.setFechaProgramada(Instant.now().plus(Duration.ofHours(ttlHours)));
        p.setEstado(MinioPendingDeletion.ESTADO_PENDIENTE);
        p.setIntentos(0);

        MinioPendingDeletion saved = repository.save(p);
        log.info("Purgado programado: id={} objectName={} motivo={} fechaProgramada={}",
                saved.getId(), objectName, motivo, saved.getFechaProgramada());
        return saved;
    }

    /**
     * Cancela la purga pendiente para un objectName. Útil para "deshacer" un reemplazo
     * o una eliminación durante el período de gracia.
     *
     * @return true si se canceló alguna entrada, false si no había nada que cancelar
     */
    @Transactional
    public boolean cancelarEliminacion(String objectName) {
        Optional<MinioPendingDeletion> existente = repository
                .findFirstByObjectNameAndEstado(objectName, MinioPendingDeletion.ESTADO_PENDIENTE);
        if (existente.isEmpty()) return false;
        MinioPendingDeletion p = existente.get();
        p.setEstado(MinioPendingDeletion.ESTADO_CANCELADO);
        repository.save(p);
        log.info("Purgado cancelado: id={} objectName={}", p.getId(), objectName);
        return true;
    }

    /**
     * Procesa las entradas vencidas: borra el archivo físico de MinIO y marca la fila como PURGADO.
     * <p>
     * Llamado por {@code MinioCleanupScheduler} cada hora. Si MinIO falla, incrementa
     * {@code intentos} y eventualmente marca la fila como FALLIDO cuando se acaban los reintentos.
     * </p>
     *
     * @return cantidad de archivos purgados exitosamente
     */
    @Transactional
    public int purgarVencidos() {
        Instant ahora = Instant.now();
        List<MinioPendingDeletion> vencidos = repository.findVencidos(ahora);
        if (vencidos.isEmpty()) {
            log.debug("purga: no hay entradas vencidas");
            return 0;
        }

        log.info("purga: {} entradas vencidas por procesar", vencidos.size());
        int purgados = 0;
        for (MinioPendingDeletion p : vencidos) {
            try {
                minioStorageService.eliminarArchivo(p.getArchivoUrl());
                p.setEstado(MinioPendingDeletion.ESTADO_PURGADO);
                p.setPurgadoAt(Instant.now());
                p.setErrorMessage(null);
                repository.save(p);
                purgados++;
                log.info("purga: archivo eliminado de MinIO id={} objectName={}", p.getId(), p.getObjectName());
            } catch (Exception e) {
                p.setIntentos(p.getIntentos() + 1);
                String msg = "Intento " + p.getIntentos() + " falló: " + e.getMessage();
                p.setErrorMessage(msg);
                if (p.getIntentos() >= maxRetries) {
                    p.setEstado(MinioPendingDeletion.ESTADO_FALLIDO);
                    log.error("purga: FALLIDO tras {} intentos id={} objectName={}",
                            p.getIntentos(), p.getId(), p.getObjectName(), e);
                } else {
                    // Mantener PENDIENTE para el próximo ciclo del scheduler.
                    log.warn("purga: {} (reintentará) id={} objectName={}", msg, p.getId(), p.getObjectName());
                }
                repository.save(p);
            }
        }
        return purgados;
    }

    @Transactional(readOnly = true)
    public long countPendientes() {
        return repository.countByEstado(MinioPendingDeletion.ESTADO_PENDIENTE);
    }

    @Transactional(readOnly = true)
    public long countPurgados() {
        return repository.countByEstado(MinioPendingDeletion.ESTADO_PURGADO);
    }

    @Transactional(readOnly = true)
    public long countFallidos() {
        return repository.countByEstado(MinioPendingDeletion.ESTADO_FALLIDO);
    }
}

package org.reciclame.demo.scheduler;

import org.reciclame.demo.model.ProgramacionBackup;
import org.reciclame.demo.model.SolicitudesRespaldo;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;
    private final UsuarioRepository usuarioRepository;

    public BackupScheduler(BackupService backupService, UsuarioRepository usuarioRepository) {
        this.backupService = backupService;
        this.usuarioRepository = usuarioRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void verificarProgramaciones() {
        List<ProgramacionBackup> programaciones = backupService.findProgramacionesActivas();

        for (ProgramacionBackup programacion : programaciones) {
            if (debeEjecutar(programacion)) {
                ejecutarBackupProgramado(programacion);
            }
        }
    }

    /**
     * Decide si una programación debe ejecutarse AHORA. Para backups de tipo
     * {@code EMERGENCIA} se ignora el flag {@code habilitado} (no se puede
     * desactivar) y la ventana es de 72h en vez de "hoy". Para los demás tipos
     * se mantiene el comportamiento legacy (diario, respeta habilitado).
     */
    private boolean debeEjecutar(ProgramacionBackup programacion) {
        boolean esEmergencia = "EMERGENCIA".equalsIgnoreCase(programacion.getTipo());

        if (!esEmergencia && !programacion.getHabilitado()) {
            return false;
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        LocalTime horaActual = ahora.toLocalTime();
        LocalTime horaProgramada = programacion.getHoraEjecucion();

        long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(horaActual, horaProgramada));
        if (diffMinutes > 1) {
            return false;
        }

        if (esEmergencia) {
            return !fueEjecutadoEnUltimasHoras(72);
        }
        return !fueEjecutadoHoy(programacion);
    }

    private boolean fueEjecutadoEnUltimasHoras(int horas) {
        Instant limite = Instant.now().minus(Duration.ofHours(horas));
        List<SolicitudesRespaldo> backupsRecientes = backupService.findByEstado("COMPLETADO");
        for (SolicitudesRespaldo backup : backupsRecientes) {
            if (backup.getCreatedAt() != null && backup.getCreatedAt().isAfter(limite)) {
                return true;
            }
        }
        return false;
    }

    private boolean fueEjecutadoHoy(ProgramacionBackup programacion) {
        LocalDateTime startOfDay = LocalDate.now(ZoneId.of("America/Lima"))
                .atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        Instant desde = startOfDay.atZone(ZoneId.of("America/Lima")).toInstant();
        Instant hasta = endOfDay.atZone(ZoneId.of("America/Lima")).toInstant();

        List<SolicitudesRespaldo> backupsHoy = backupService.findByEstado("COMPLETADO");

        for (SolicitudesRespaldo backup : backupsHoy) {
            if (backup.getCreatedAt() != null &&
                backup.getCreatedAt().isAfter(desde) &&
                backup.getCreatedAt().isBefore(hasta)) {
                return true;
            }
        }

        return false;
    }

    private void ejecutarBackupProgramado(ProgramacionBackup programacion) {
        log.info("Ejecutando backup programado: {} - {}", programacion.getTipo(), programacion.getFrecuencia());

        boolean esEmergencia = "EMERGENCIA".equalsIgnoreCase(programacion.getTipo());

        // Si es un backup de emergencia, primero limpiamos el anterior para que
        // solo exista 1 archivo a la vez. No aborta si falla: la prioridad es
        // tener un backup fresco.
        if (esEmergencia) {
            try {
                Long idAnterior = backupService.eliminarBackupEmergenciaAnterior();
                if (idAnterior != null) {
                    log.info("Backup de emergencia anterior (ID={}) marcado para reemplazo", idAnterior);
                }
            } catch (Exception e) {
                log.warn("No se pudo limpiar backup emergencia anterior (continúa con el nuevo): {}", e.getMessage());
            }
        }

        Usuario superAdmin = usuarioRepository.findById(1L).orElse(null);
        if (superAdmin == null) {
            superAdmin = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol().getNombre().equals("SUPERADMIN"))
                    .findFirst()
                    .orElse(null);
        }

        if (superAdmin == null) {
            log.error("No se encontró usuario SuperAdmin para ejecutar backup programado");
            return;
        }

        try {
            String notas = "Backup automático - " + programacion.getFrecuencia() +
                          " (" + LocalDate.now(ZoneId.of("America/Lima")) + ")";

            SolicitudesRespaldo backup = backupService.crearBackupManual(
                    superAdmin,
                    programacion.getTipo(),
                    notas
            );

            backupService.ejecutarBackupAsync(backup);

            log.info("Backup programado {} creado exitosamente (tipo={})", backup.getId(), programacion.getTipo());

        } catch (Exception e) {
            log.error("Error ejecutando backup programado: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelay = 3600000)
    public void verificarProgramacionDiariaDefault() {
        if (backupService.findAllProgramaciones().isEmpty()) {
            crearProgramacionDefault();
        }
    }

    private void crearProgramacionDefault() {
        try {
            ProgramacionBackup defaultProg = new ProgramacionBackup();
            // Política actual: 1 backup de emergencia cada 72h, no desactivable.
            // El archivo se llama siempre backup_emergencia.sql y sobreescribe
            // al anterior (solo 1 archivo en disco a la vez).
            defaultProg.setTipo("EMERGENCIA");
            defaultProg.setFrecuencia("CADA_72H");
            defaultProg.setHoraEjecucion(LocalTime.of(2, 0));
            defaultProg.setHabilitado(true);
            defaultProg.setEstadoActivo(true);

            backupService.crearProgramacion(defaultProg);
            log.info("Programación de backup de emergencia creada por defecto (cada 72h, 02:00, no desactivable)");
        } catch (Exception e) {
            log.warn("No se pudo crear programación default: {}", e.getMessage());
        }
    }
}
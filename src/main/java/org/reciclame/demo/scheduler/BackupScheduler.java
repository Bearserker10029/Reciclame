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

    private boolean debeEjecutar(ProgramacionBackup programacion) {
        if (!programacion.getHabilitado()) {
            return false;
        }

        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        LocalTime horaActual = ahora.toLocalTime();
        LocalTime horaProgramada = programacion.getHoraEjecucion();

        long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(horaActual, horaProgramada));
        if (diffMinutes > 1) {
            return false;
        }

        if (!fueEjecutadoHoy(programacion)) {
            return true;
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

            log.info("Backup programado {} creado exitosamente", backup.getId());

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
            defaultProg.setTipo("COMPLETO");
            defaultProg.setFrecuencia("DIARIA");
            defaultProg.setHoraEjecucion(LocalTime.of(2, 0));
            defaultProg.setHabilitado(true);
            defaultProg.setEstadoActivo(true);

            backupService.crearProgramacion(defaultProg);
            log.info("Programación de backup diario creada por defecto (02:00)");
        } catch (Exception e) {
            log.warn("No se pudo crear programación default: {}", e.getMessage());
        }
    }
}
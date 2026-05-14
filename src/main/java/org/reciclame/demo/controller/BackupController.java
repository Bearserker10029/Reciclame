package org.reciclame.demo.controller;

import org.reciclame.demo.model.ProgramacionBackup;
import org.reciclame.demo.model.SolicitudesRespaldo;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.BackupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/respaldos")
public class BackupController {

    private final BackupService backupService;
    private final UsuarioRepository usuarioRepository;

    public BackupController(BackupService backupService, UsuarioRepository usuarioRepository) {
        this.backupService = backupService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarBackups() {
        List<SolicitudesRespaldo> backups = backupService.findAll();
        return ResponseEntity.ok(backups.stream().map(this::toMap).toList());
    }

    @GetMapping("/completados")
    public ResponseEntity<List<Map<String, Object>>> listarCompletados() {
        List<SolicitudesRespaldo> backups = backupService.findCompletados();
        return ResponseEntity.ok(backups.stream().map(this::toMap).toList());
    }

    @GetMapping("/en-proceso")
    public ResponseEntity<List<Map<String, Object>>> listarEnProceso() {
        List<SolicitudesRespaldo> backups = backupService.findEnProceso();
        return ResponseEntity.ok(backups.stream().map(this::toMap).toList());
    }

    @GetMapping("/fallidos")
    public ResponseEntity<List<Map<String, Object>>> listarFallidos() {
        List<SolicitudesRespaldo> backups = backupService.findFallidos();
        return ResponseEntity.ok(backups.stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerBackup(@PathVariable Long id) {
        SolicitudesRespaldo backup = backupService.findById(id);
        if (backup == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toMap(backup));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crearBackup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "COMPLETO") String tipo,
            @RequestParam(required = false) String notas) {

        Usuario solicitante = usuarioRepository.findByCorreoCorporativo(userDetails.getUsername()).orElse(null);
        if (solicitante == null) {
            return ResponseEntity.status(401).build();
        }

        SolicitudesRespaldo backup = backupService.crearBackupManual(solicitante, tipo, notas);
        backupService.ejecutarBackupAsync(backup);

        Map<String, Object> response = new HashMap<>();
        response.put("id", backup.getId());
        response.put("estado", backup.getEstado());
        response.put("mensaje", "Backup solicitado exitosamente");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reintentar")
    public ResponseEntity<Map<String, Object>> reintentarBackup(@PathVariable Long id) {
        try {
            backupService.reintentarBackup(id);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Backup reintentado exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/restaurar")
    public ResponseEntity<Map<String, Object>> restaurarBackup(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "true") boolean crearBackupSeguridad) {

        Usuario usuario = usuarioRepository.findByCorreoCorporativo(userDetails.getUsername()).orElse(null);
        if (usuario == null || !"SUPERADMIN".equals(usuario.getRol().getNombre())) {
            return ResponseEntity.status(403).build();
        }

        try {
            backupService.restaurarBackup(id, usuario, crearBackupSeguridad);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Restauración completada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarBackup(@PathVariable Long id) {
        try {
            backupService.eliminarBackup(id);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Backup eliminado exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> obtenerStats() {
        BackupService.BackupStats stats = backupService.getStats();
        Map<String, Object> response = new HashMap<>();
        response.put("total", stats.total());
        response.put("completados", stats.completados());
        response.put("enProceso", stats.enProceso());
        response.put("fallidos", stats.fallidos());
        if (stats.ultimoCompletado() != null) {
            response.put("ultimoBackup", formatInstant(stats.ultimoCompletado().getFechaCompletado()));
        } else {
            response.put("ultimoBackup", null);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/programaciones")
    public ResponseEntity<List<Map<String, Object>>> listarProgramaciones() {
        List<ProgramacionBackup> programaciones = backupService.findAllProgramaciones();
        return ResponseEntity.ok(programaciones.stream().map(this::programacionToMap).toList());
    }

    @PostMapping("/programaciones")
    public ResponseEntity<Map<String, Object>> crearProgramacion(
            @RequestBody Map<String, Object> body) {
        try {
            ProgramacionBackup programacion = new ProgramacionBackup();
            programacion.setTipo((String) body.get("tipo"));
            programacion.setFrecuencia((String) body.get("frecuencia"));

            String horaStr = (String) body.get("horaEjecucion");
            if (horaStr != null && !horaStr.isEmpty()) {
                programacion.setHoraEjecucion(java.time.LocalTime.parse(horaStr));
            } else {
                programacion.setHoraEjecucion(java.time.LocalTime.of(2, 0));
            }

            if (body.get("diaSemana") != null) {
                programacion.setDiaSemana((Integer) body.get("diaSemana"));
            }
            if (body.get("diaMes") != null) {
                programacion.setDiaMes((Integer) body.get("diaMes"));
            }
            if (body.get("habilitado") != null) {
                programacion.setHabilitado((Boolean) body.get("habilitado"));
            } else {
                programacion.setHabilitado(true);
            }

            ProgramacionBackup creada = backupService.crearProgramacion(programacion);
            Map<String, Object> response = new HashMap<>();
            response.put("id", creada.getId());
            response.put("mensaje", "Programación creada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/programaciones/{id}")
    public ResponseEntity<Map<String, Object>> actualizarProgramacion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            ProgramacionBackup programacion = new ProgramacionBackup();
            programacion.setTipo((String) body.get("tipo"));
            programacion.setFrecuencia((String) body.get("frecuencia"));

            String horaStr = (String) body.get("horaEjecucion");
            if (horaStr != null && !horaStr.isEmpty()) {
                programacion.setHoraEjecucion(java.time.LocalTime.parse(horaStr));
            } else {
                programacion.setHoraEjecucion(java.time.LocalTime.of(2, 0));
            }

            if (body.get("diaSemana") != null) {
                programacion.setDiaSemana((Integer) body.get("diaSemana"));
            }
            if (body.get("diaMes") != null) {
                programacion.setDiaMes((Integer) body.get("diaMes"));
            }
            if (body.get("habilitado") != null) {
                programacion.setHabilitado((Boolean) body.get("habilitado"));
            }

            ProgramacionBackup actualizada = backupService.actualizarProgramacion(id, programacion);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Programación actualizada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/programaciones/{id}")
    public ResponseEntity<Map<String, Object>> eliminarProgramacion(@PathVariable Long id) {
        try {
            backupService.eliminarProgramacion(id);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Programación eliminada exitosamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/programaciones/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleProgramacion(@PathVariable Long id) {
        try {
            ProgramacionBackup toggled = backupService.toggleProgramacion(id);
            Map<String, Object> response = new HashMap<>();
            response.put("habilitado", toggled.getHabilitado());
            response.put("mensaje", toggled.getHabilitado() ? "Programación activada" : "Programación pausada");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, Object> toMap(SolicitudesRespaldo backup) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", backup.getId());
        map.put("tipo", backup.getTipo());
        map.put("estado", backup.getEstado());
        map.put("storageKey", backup.getStorageKey());
        map.put("tamanioBytes", backup.getTamanioBytes());
        map.put("tamanioFormateado", formatearBytes(backup.getTamanioBytes()));
        map.put("notas", backup.getNotas());
        map.put("esBackupPreRestauracion", backup.getEsBackupPreRestauracion());
        map.put("fechaCompletado", formatInstant(backup.getFechaCompletado()));
        map.put("fechaRestauracion", formatInstant(backup.getFechaRestauracion()));
        map.put("restauradoPor", backup.getRestauradoPor() != null ? backup.getRestauradoPor().getNombreCompleto() : null);
        map.put("createdAt", formatInstant(backup.getCreatedAt()));
        map.put("solicitadoPor", backup.getSolicitadoPor() != null ? backup.getSolicitadoPor().getNombreCompleto() : null);
        return map;
    }

    private Map<String, Object> programacionToMap(ProgramacionBackup p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("tipo", p.getTipo());
        map.put("frecuencia", p.getFrecuencia());
        map.put("horaEjecucion", p.getHoraEjecucion() != null ? p.getHoraEjecucion().toString() : "02:00");
        map.put("diaSemana", p.getDiaSemana());
        map.put("diaMes", p.getDiaMes());
        map.put("habilitado", p.getHabilitado());
        map.put("createdAt", formatInstant(p.getCreatedAt()));
        return map;
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return null;
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.of("America/Lima"))
                .format(instant);
    }

    private String formatearBytes(Long bytes) {
        if (bytes == null) return "—";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
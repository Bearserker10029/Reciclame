package org.reciclame.demo.controller;

import org.reciclame.demo.dto.UploadTaskStatusDTO;
import org.reciclame.demo.helper.EstadoValidacion;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.service.UploadStagingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints REST para que el frontend haga polling del estado de validación de los
 * archivos subidos vía flujo asíncrono (caché staging → VirusTotal → MinIO docs).
 *
 * <p>El frontend invoca {@code GET /api/uploads/{tipo}/{taskId}} cada ~3 segundos sobre
 * las filas marcadas como PENDIENTE. Cuando el estado deja de ser PENDIENTE, se elimina
 * el listener y se actualiza el badge en sitio.</p>
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadStatusRestController {

    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final UploadStagingService stagingService;

    public UploadStatusRestController(NormativaRepository normativaRepository,
                                      EstudioRepository estudioRepository,
                                      UploadStagingService stagingService) {
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.stagingService = stagingService;
    }

    // Antes isAuthenticated() a secas: cualquier usuario autenticado (incluido
    // Visualizador, que ni siquiera puede subir documentos) podía consultar el
    // estado/veredicto VT de una subida ajena adivinando el taskId. Solo
    // Admin/SuperAdmin suben documentos y son los únicos que llaman esto (ver
    // Admin/SuperAdmin estudios.html y normativo.html) — mismo alcance que el
    // DELETE de acá abajo.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{tipo}/{taskId}")
    public ResponseEntity<?> status(@PathVariable String tipo, @PathVariable String taskId) {
        String tipoUpper = tipo.toUpperCase();
        Optional<UploadTaskStatusDTO> result = switch (tipoUpper) {
            case "NORMATIVA" -> buscarNormativa(taskId);
            case "ESTUDIO"   -> buscarEstudio(taskId);
            default -> Optional.empty();
        };
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró el recurso " + tipo + " con taskId=" + taskId));
        }
        return ResponseEntity.ok(result.get());
    }

    /**
     * Cancelación manual de un archivo pendiente. Solo ADMIN/SUPERADMIN. Útil si el
     * admin sube por error y quiere abortar la validación.
     */
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @DeleteMapping("/{tipo}/{taskId}")
    public ResponseEntity<?> cancelar(@PathVariable String tipo, @PathVariable String taskId) {
        String tipoUpper = tipo.toUpperCase();
        Optional<UploadTaskStatusDTO> opt = switch (tipoUpper) {
            case "NORMATIVA" -> buscarNormativa(taskId);
            case "ESTUDIO"   -> buscarEstudio(taskId);
            default -> Optional.empty();
        };
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", "No se encontró el recurso pendiente " + tipo + " con taskId=" + taskId));
        }
        UploadTaskStatusDTO current = opt.get();
        if (current.getEstado() != org.reciclame.demo.helper.EstadoValidacion.PENDIENTE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("mensaje", "El archivo ya no está pendiente (estado=" + current.getEstado() + ")"));
        }
        // Marcar como inactivo para que no aparezca en listados; limpiar staging en MinIO.
        if ("NORMATIVA".equals(tipoUpper)) {
            Normativa n = normativaRepository.findById(current.getRefId()).orElse(null);
            if (n != null) {
                n.setEstadoActivo(false);
                n.setUpdatedAt(Instant.now());
                normativaRepository.save(n);
            }
        } else {
            Estudio e = estudioRepository.findById(current.getRefId()).orElse(null);
            if (e != null) {
                e.setEstadoActivo(false);
                e.setUpdatedAt(Instant.now());
                estudioRepository.save(e);
            }
        }
        stagingService.deleteByObjectName(taskId);
        return ResponseEntity.ok(Map.of("mensaje", "Cancelado", "taskId", taskId));
    }

    // ── Helpers ──────────────────────────────────────────────

    private Optional<UploadTaskStatusDTO> buscarNormativa(String stagingObject) {
        // Buscamos por archivo_url = 'staging://<stagingObject>'
        String expected = EstadoValidacion.stagingMarker(stagingObject);
        return normativaRepository.findAll().stream()
                .filter(n -> expected.equals(n.getArchivoUrl()))
                .findFirst()
                .map(n -> UploadTaskStatusDTO.builder()
                        .tipo("NORMATIVA")
                        .taskId(stagingObject)
                        .refId(n.getId())
                        .estado(EstadoValidacion.inferir(n.getArchivoUrl(), n.getVtVerdict()))
                        .archivoUrl(n.getArchivoUrl())
                        .vtVerdict(n.getVtVerdict())
                        .vtTotalEngines(n.getVtTotalEngines())
                        .vtMaliciousEngines(n.getVtMaliciousEngines())
                        .vtSuspiciousEngines(n.getVtSuspiciousEngines())
                        .vtSkipped(n.getVtSkipped())
                        .message(n.getVtScannedAt() != null ? n.getVtScannedAt().toString() : null)
                        .updatedAt(n.getUpdatedAt())
                        .build());
    }

    private Optional<UploadTaskStatusDTO> buscarEstudio(String stagingObject) {
        String expected = EstadoValidacion.stagingMarker(stagingObject);
        return estudioRepository.findAll().stream()
                .filter(e -> expected.equals(e.getArchivoUrl()))
                .findFirst()
                .map(e -> UploadTaskStatusDTO.builder()
                        .tipo("ESTUDIO")
                        .taskId(stagingObject)
                        .refId(e.getId())
                        .estado(EstadoValidacion.inferir(e.getArchivoUrl(), e.getVtVerdict()))
                        .archivoUrl(e.getArchivoUrl())
                        .vtVerdict(e.getVtVerdict())
                        .vtTotalEngines(e.getVtTotalEngines())
                        .vtMaliciousEngines(e.getVtMaliciousEngines())
                        .vtSuspiciousEngines(e.getVtSuspiciousEngines())
                        .vtSkipped(e.getVtSkipped())
                        .message(e.getVtScannedAt() != null ? e.getVtScannedAt().toString() : null)
                        .updatedAt(e.getUpdatedAt())
                        .build());
    }
}

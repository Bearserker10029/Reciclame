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

// El frontend hace polling acá mientras un archivo recién subido está
// "pendiente" (staging + escaneo VirusTotal en curso, ver UploadStagingScanner).
// Ojo: no hay job de recuperación automática si el escaneo se cae a medias —
// si un registro se queda pendiente para siempre, cancelar() es la única
// forma de destrabarlo desde acá.
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


    private Optional<UploadTaskStatusDTO> buscarNormativa(String stagingObject) {
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

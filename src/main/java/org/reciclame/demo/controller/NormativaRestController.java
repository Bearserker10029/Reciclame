package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.SubirNormativaStagingService;
import org.reciclame.demo.service.UsuarioService;
import org.reciclame.demo.util.HttpClientIpResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

// CRUD de normativas para Admin/SuperAdmin. Ojo con crear/actualizar: si viene
// archivo, esto NO guarda la normativa todavía — la manda a staging (202
// Accepted) y el registro final queda pendiente hasta que el escaneo
// antivirus la apruebe (ver UploadStagingScanner / api/uploads para el polling).
@RestController
@RequestMapping("/api/normativas")
public class NormativaRestController {

    private final NormativaService normativaService;
    private final SubirNormativaStagingService subirStagingService;
    private final UsuarioService usuarioService;

    public NormativaRestController(NormativaService normativaService,
                                   SubirNormativaStagingService subirStagingService,
                                   UsuarioService usuarioService) {
        this.normativaService = normativaService;
        this.subirStagingService = subirStagingService;
        this.usuarioService = usuarioService;
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping
    public ResponseEntity<List<NormativaResponseDTO>> listar() {
        return ResponseEntity.ok(normativaService.listarNormativas());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(normativaService.obtenerNormativaPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest request) throws java.io.IOException {
        var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
        Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
        String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            if (archivo != null && !archivo.isEmpty()) {
                subirStagingService.validarExtension(archivo);
                SubirNormativaStagingService.ResultadoSubida resultado =
                        subirStagingService.subirYPendientarNormativa(dto, archivo,
                                user.getUsername(), auditorId, auditorNombre, ipAddress, userAgent);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                        "taskId", resultado.getTaskId(),
                        "tipo", "NORMATIVA",
                        "refId", resultado.getRefId(),
                        "statusUrl", "/api/uploads/NORMATIVA/" + resultado.getTaskId(),
                        "estado", "PENDIENTE",
                        "mensaje", "Subida recibida. La validación antivirus está en curso."
                ));
            }
            NormativaResponseDTO resultado = normativaService.crearNormativa(
                    dto, null, null,
                    user.getUsername(),
                    auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest request) throws java.io.IOException {
        var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
        Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
        String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        try {
            if (archivo != null && !archivo.isEmpty()) {
                SubirNormativaStagingService.ResultadoSubida resultado =
                        subirStagingService.subirYPendientarNormativaEnUpdate(id, dto, archivo,
                                user.getUsername(), auditorId, auditorNombre, ipAddress, userAgent);
                return ResponseEntity.accepted().body(Map.of(
                        "taskId", resultado.getTaskId(),
                        "tipo", "NORMATIVA",
                        "refId", resultado.getRefId(),
                        "statusUrl", "/api/uploads/NORMATIVA/" + resultado.getTaskId(),
                        "estado", "PENDIENTE",
                        "mensaje", "Archivo en validación. Los cambios se aplicarán al aprobarse."
                ));
            }
            NormativaResponseDTO resultado = normativaService.actualizarNormativa(
                    id, dto, null, null,
                    auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails user,
                                      HttpServletRequest request) {
        try {
            var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
            Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
            String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
            String ipAddress = HttpClientIpResolver.resolve(request);
            String userAgent = request.getHeader("User-Agent");
            normativaService.eliminarNormativa(id, auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }
}

package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.service.MinioStorageService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.UsuarioService;
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

@RestController
@RequestMapping("/api/normativas")
public class NormativaRestController {

    private final NormativaService normativaService;
    private final MinioStorageService minioStorageService;
    private final UsuarioService usuarioService;

    public NormativaRestController(NormativaService normativaService,
                                   MinioStorageService minioStorageService,
                                   UsuarioService usuarioService) {
        this.normativaService = normativaService;
        this.minioStorageService = minioStorageService;
        this.usuarioService = usuarioService;
    }

    // ── READ ALL ──────────────────────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NormativaResponseDTO>> listar() {
        return ResponseEntity.ok(normativaService.listarNormativas());
    }

    // ── READ ONE ──────────────────────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(normativaService.obtenerNormativaPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest request) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
            Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
            String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            NormativaResponseDTO resultado = normativaService.crearNormativa(
                    dto, archivoUrl, archivoTipoMime, user.getUsername(),
                    auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── UPDATE ────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user,
            HttpServletRequest request) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
            Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
            String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            NormativaResponseDTO resultado = normativaService.actualizarNormativa(
                    id, dto, archivoUrl, archivoTipoMime,
                    auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── DELETE lógico ─────────────────────────────────────────
    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails user,
                                      HttpServletRequest request) {
        try {
            var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
            Long auditorId = usuarioOpt.map(u -> u.getId()).orElse(null);
            String auditorNombre = usuarioOpt.map(u -> u.getNombreCompleto()).orElse(user.getUsername());
            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            normativaService.eliminarNormativa(id, auditorId, auditorNombre, ipAddress, userAgent);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Subida a MinIO ────────────────────────────────────────
    private String simularSubida(MultipartFile archivo) {
        return minioStorageService.subirArchivo(archivo);
    }
}

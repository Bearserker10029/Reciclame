package org.reciclame.demo.controller;

import jakarta.validation.Valid;
import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
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
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public NormativaRestController(NormativaService normativaService, NotificacionService notificacionService,
                                    AuditoriaService auditoriaService, UsuarioService usuarioService) {
        this.normativaService = normativaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
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
            @AuthenticationPrincipal UserDetails user) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            NormativaResponseDTO resultado = normativaService.crearNormativa(
                    dto, archivoUrl, archivoTipoMime, user.getUsername());
            String titulo = "Nueva Normativa Publicada";
            String mensaje = "Se ha publicado una nueva normativa: " + dto.getTitulo();
            notificacionService.enviarNotificacionGlobal(titulo, mensaje, "NUEVA_NORMATIVA");
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
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            NormativaResponseDTO resultado = normativaService.actualizarNormativa(
                    id, dto, archivoUrl, archivoTipoMime);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── DELETE lógico ─────────────────────────────────────────
    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            normativaService.eliminarNormativa(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Registrar visualización ───────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/visualizar")
    public ResponseEntity<?> registrarVisualizacion(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails user) {
        try {
            int nuevoCuenta = normativaService.incrementarVisualizaciones(id);
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                auditoriaService.registrarAccion(u.getId(), u.getNombreCompleto(),
                    AuditoriaService.ACCION_VER_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                    "Visualizó normativa id=" + id));
            return ResponseEntity.ok(Map.of("visualizacionesCount", nuevoCuenta));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Registrar descarga ────────────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/descargar")
    public ResponseEntity<?> registrarDescarga(@PathVariable Long id,
                                                @AuthenticationPrincipal UserDetails user) {
        try {
            int nuevoCuenta = normativaService.incrementarDescargas(id);
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                auditoriaService.registrarAccion(u.getId(), u.getNombreCompleto(),
                    AuditoriaService.ACCION_DESCARGAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                    "Descargó normativa id=" + id));
            return ResponseEntity.ok(Map.of("descargasCount", nuevoCuenta));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Stub subida (reemplazar con S3/GCP) ───────────────────
    private String simularSubida(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        return "https://storage.googleapis.com/reciclame-docs/"
                + System.currentTimeMillis() + "_" + nombre;
    }
}

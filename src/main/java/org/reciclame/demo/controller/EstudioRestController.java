package org.reciclame.demo.controller;

import jakarta.validation.Valid;
import org.reciclame.demo.dto.EstudioRequestDTO;
import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.MinioStorageService;
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
@RequestMapping("/api/estudios")
public class EstudioRestController {

    private final EstudioService estudioService;
    private final MinioStorageService minioStorageService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public EstudioRestController(EstudioService estudioService, MinioStorageService minioStorageService,
                                  NotificacionService notificacionService, AuditoriaService auditoriaService,
                                  UsuarioService usuarioService) {
        this.estudioService = estudioService;
        this.minioStorageService = minioStorageService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    // ── READ ALL ──────────────────────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<EstudioResponseDTO>> listar() {
        return ResponseEntity.ok(estudioService.listarEstudios());
    }

    // ── READ ONE ──────────────────────────────────────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudioService.obtenerEstudioPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    // @Valid sobre @RequestPart funciona porque el JS envía el Blob con
    // Content-Type: application/json → Spring deserializa + valida.
    // Si falla la validación, MethodArgumentNotValidException es lanzada
    // ANTES de entrar al método → GlobalExceptionHandler la captura.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("estudio") @Valid EstudioRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            EstudioResponseDTO resultado = estudioService.crearEstudio(
                    dto, archivoUrl, archivoTipoMime, user.getUsername());
            String titulo = "Nuevo Estudio Publicado";
            String mensaje = "Se ha publicado un nuevo estudio: " + dto.getTitulo();
            notificacionService.enviarNotificacionGlobal(titulo, mensaje, "NUEVO_ESTUDIO");
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
            @RequestPart("estudio") @Valid EstudioRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            EstudioResponseDTO resultado = estudioService.actualizarEstudio(
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
            estudioService.eliminarEstudio(id);
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
            int nuevoCuenta = estudioService.incrementarVisualizaciones(id);
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                auditoriaService.registrarAccion(u.getId(), u.getNombreCompleto(),
                    AuditoriaService.ACCION_VER_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
                    "Visualizó estudio id=" + id));
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
            int nuevoCuenta = estudioService.incrementarDescargas(id);
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                auditoriaService.registrarAccion(u.getId(), u.getNombreCompleto(),
                    AuditoriaService.ACCION_DESCARGAR_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
                    "Descargó estudio id=" + id));
            return ResponseEntity.ok(Map.of("descargasCount", nuevoCuenta));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Subida a MinIO ────────────────────────────────────────
    private String simularSubida(MultipartFile archivo) {
        return minioStorageService.subirArchivo(archivo);
    }
}

package org.reciclame.demo.controller;

import org.reciclame.demo.dto.UsuarioRequestDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.UsuarioService;
import org.reciclame.demo.util.HttpClientIpResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AuditoriaService auditoriaService;

    public UsuarioController(UsuarioService usuarioService, AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.auditoriaService = auditoriaService;
    }

    // CREATE
    // POST /api/usuarios
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(@RequestBody UsuarioRequestDTO dto,
            HttpServletRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String auditorUsuarioNombre = userDetails != null ? userDetails.getUsername() : "Sistema";
        UsuarioResponseDTO auditor = auditorUsuarioNombre != null ?
                usuarioService.findByCorreo(auditorUsuarioNombre).orElse(null) : null;
        Long auditorUsuarioId = auditor != null ? auditor.getId() : null;
        if (auditor != null) auditorUsuarioNombre = auditor.getNombreCompleto();

        UsuarioResponseDTO nuevo = usuarioService.crearUsuario(dto, auditorUsuarioId, auditorUsuarioNombre, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // READ ALL
    // GET /api/usuarios
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    //  READ ONE
    // GET /api/usuarios/{id}
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    // UPDATE
    // PUT /api/usuarios/{id}
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody UsuarioRequestDTO dto,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    // DELETE (lógico)
    // DELETE /api/usuarios/{id}
    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id, HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        usuarioService.eliminarUsuario(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    // APROBAR solicitud pendiente
    // POST /api/usuarios/{id}/aprobar?rolId=X&empresaId=Y
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<UsuarioResponseDTO> aprobarUsuario(
            @PathVariable Long id,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) Long empresaId,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (rolId == null) {
            return ResponseEntity.badRequest().build();
        }
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.aprobarUsuario(id, rolId, empresaId, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    // RECHAZAR solicitud pendiente
    // POST /api/usuarios/{id}/rechazar
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<UsuarioResponseDTO> rechazarUsuario(@PathVariable Long id,
            @RequestParam(required = false) String motivo,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.rechazarUsuario(id, motivo, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    // BLOQUEAR / DESBLOQUEAR usuario activo
    // POST /api/usuarios/{id}/bloquear
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/bloquear")
    public ResponseEntity<UsuarioResponseDTO> bloquearUsuario(@PathVariable Long id, HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.toggleBloquear(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    // REACTIVAR USUARIO
    // POST /api/usuarios/{id}/reactivar
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<?> reaccionarUsuario(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        String[] auditor = resolverAuditor(userDetails);
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(usuarioService.reactivarUsuario(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    // HISTORIAL DE ACTIVIDAD POR USUARIO
    // GET /api/usuarios/{id}/historial
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{id}/historial")
    public ResponseEntity<List<Map<String, Object>>> historialUsuario(@PathVariable Long id) {
        List<Auditoria> auditorias = auditoriaService.listarAuditoriasPorUsuario(id);
        List<Map<String, Object>> result = auditorias.stream()
                .filter(a -> a.getEstadoActivo() == null || a.getEstadoActivo())
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("accion", a.getAccion());
                    m.put("modulo", a.getModulo());
                    m.put("descripcion", a.getDescripcion());
                    m.put("estado", a.getEstado());
                    m.put("ipAddress", a.getIpAddress());
                    m.put("fechaAccion", a.getFechaAccion() != null ? a.getFechaAccion().toString() : null);
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    // EXPORTAR CSV
    // GET /api/usuarios/export.csv
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportarCsv() {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"usuarios.csv\"")
                .body(usuarioService.exportarCsv());
    }

    // ── HELPER METHODS ────────────────────────────────────────

    // Retorna [id, nombreCompleto] del usuario autenticado
    private String[] resolverAuditor(UserDetails userDetails) {
        if (userDetails == null) return new String[]{null, "Sistema"};
        UsuarioResponseDTO dto = usuarioService.findByCorreo(userDetails.getUsername()).orElse(null);
        if (dto == null) return new String[]{null, userDetails.getUsername()};
        return new String[]{String.valueOf(dto.getId()), dto.getNombreCompleto()};
    }

    // ── MANEJO DE ERRORES ─────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("correo_corporativo")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un usuario con ese correo corporativo.");
        }
        if (msg != null && msg.contains("dni")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Ya existe un usuario con ese DNI.");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Error de integridad: " + msg);
    }
}

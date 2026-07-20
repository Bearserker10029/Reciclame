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

// Gestión de usuarios para Admin/SuperAdmin. Casi todos los endpoints
// mandan quién hizo la acción (id + nombre) al service para auditoría —
// de ahí el resolverAuditor() repetido por todos lados. Bloquear/cambiar
// rol/desactivar invalida la sesión activa del usuario afectado (lo hace
// el service, no acá). Borrado físico (eliminarUsuario) es el único
// endpoint restringido solo a SUPERADMIN; el resto acepta también ADMIN.
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

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody UsuarioRequestDTO dto,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UsuarioResponseDTO actual = usuarioService.obtenerUsuarioPorId(id);
        if (actual != null && actual.getEstadoActivo() != null && !actual.getEstadoActivo()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "USUARIO_INACTIVO");
            err.put("mensaje", "No se puede editar un usuario inactivo. Reactivarlo primero.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    @PreAuthorize("hasRole('SUPERADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id, HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UsuarioResponseDTO actual = usuarioService.obtenerUsuarioPorId(id);
        if (actual != null && actual.getEstadoActivo() != null && !actual.getEstadoActivo()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "USUARIO_INACTIVO");
            err.put("mensaje", "El usuario ya está inactivo. No es necesario eliminarlo.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        usuarioService.eliminarUsuario(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

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

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/bloquear")
    public ResponseEntity<?> bloquearUsuario(@PathVariable Long id, HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UsuarioResponseDTO actual = usuarioService.obtenerUsuarioPorId(id);
        if (actual != null && actual.getEstadoActivo() != null && !actual.getEstadoActivo()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "USUARIO_INACTIVO");
            err.put("mensaje", "No se puede bloquear/desbloquear un usuario inactivo. Reactivarlo primero.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String[] auditor = resolverAuditor(userDetails);
        return ResponseEntity.ok(usuarioService.toggleBloquear(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<?> reaccionarUsuario(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        String[] auditor = resolverAuditor(userDetails);
        String ipAddress = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        return ResponseEntity.ok(usuarioService.reactivarUsuario(id, auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1], ipAddress, userAgent));
    }

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

    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportarCsv() {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"usuarios.csv\"")
                .body(usuarioService.exportarCsv());
    }


    private String[] resolverAuditor(UserDetails userDetails) {
        if (userDetails == null) return new String[]{null, "Sistema"};
        UsuarioResponseDTO dto = usuarioService.findByCorreo(userDetails.getUsername()).orElse(null);
        if (dto == null) return new String[]{null, userDetails.getUsername()};
        return new String[]{String.valueOf(dto.getId()), dto.getNombreCompleto()};
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        // Auto-acciones (administrador modificando su propia cuenta desde el
        // panel admin) → 403 Forbidden. La UI usa este código para mostrar el
        // mensaje como "no permitido" en vez de "datos inválidos".
        HttpStatus status = msg.startsWith("No puedes ") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status == HttpStatus.FORBIDDEN ? "AUTO_ACCION_DENEGADA" : "BAD_REQUEST");
        body.put("mensaje", msg);
        return ResponseEntity.status(status).body(body);
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

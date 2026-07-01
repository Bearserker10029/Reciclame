package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.reciclame.demo.dto.SolicitudRegistroDTO;
import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.SolicitudesRegistro;
import org.reciclame.demo.model.Empresa;
import org.reciclame.demo.model.Rol;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.EmpresaRepository;
import org.reciclame.demo.repository.RolRepository;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EmailNotificationService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.reciclame.demo.util.HttpClientIpResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final SolicitudesRegistroRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final EmpresaRepository empresaRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public SolicitudController(SolicitudesRegistroRepository solicitudRepo,
                               UsuarioRepository usuarioRepo,
                               RolRepository rolRepo,
                               EmpresaRepository empresaRepo,
                               PasswordEncoder passwordEncoder,
                               EmailNotificationService emailService,
                               NotificacionService notificacionService,
                               AuditoriaService auditoriaService,
                               UsuarioService usuarioService) {
        this.solicitudRepo = solicitudRepo;
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.empresaRepo = empresaRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    // POST /api/solicitudes/{id}/aprobar?rolId=X&empresaId=Y
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(
            @PathVariable Long id,
            @RequestParam Long rolId,
            @RequestParam(required = false) Long empresaId,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        SolicitudesRegistro solicitud = solicitudRepo.findById(id)
                .orElse(null);
        if (solicitud == null)
            return ResponseEntity.notFound().build();
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE)
            return ResponseEntity.badRequest().body("La solicitud ya fue procesada");

        // Evitar duplicados de correo o DNI
        if (usuarioRepo.existsByCorreoCorporativo(solicitud.getCorreoCorporativo()))
            return ResponseEntity.badRequest().body("Ya existe un usuario con ese correo");
        if (usuarioRepo.existsByDni(solicitud.getDni()))
            return ResponseEntity.badRequest().body("Ya existe un usuario con ese DNI");

        Rol rol = rolRepo.findById(rolId)
                .orElse(null);
        if (rol == null)
            return ResponseEntity.badRequest().body("Rol no encontrado");

        Empresa empresa = null;
        if (empresaId != null)
            empresa = empresaRepo.findById(empresaId).orElse(null);

        // Generar contraseña temporal
        String tempPassword = generarPassword(10);

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(solicitud.getNombreCompleto());
        usuario.setDni(solicitud.getDni());
        usuario.setCorreoCorporativo(solicitud.getCorreoCorporativo());
        usuario.setPasswordHash(passwordEncoder.encode(tempPassword));
        usuario.setRol(rol);
        usuario.setEmpresa(empresa);
        usuario.setEstadoCuenta("ACTIVO");
        usuario.setEstadoActivo(true);
        usuario.setCuentaBloqueada(false);
        usuario.setTfaActivo(false);
        usuario.setIntentosFallidos((byte) 0);
        usuario.setCreatedAt(Instant.now());
        usuario.setUpdatedAt(Instant.now());
        Usuario guardado = usuarioRepo.save(usuario);

        // Marcar solicitud como aprobada
        solicitud.setEstado(EstadoSolicitud.APROBADA);
        solicitud.setFechaResolucion(Instant.now());
        if (userDetails != null) {
            usuarioService.findByCorreo(userDetails.getUsername())
                    .map(dto -> usuarioRepo.findById(dto.getId()).orElse(null))
                    .ifPresent(solicitud::setRevisadaPor);
        }
        solicitudRepo.save(solicitud);

        // Auditoría
        String[] auditor = resolverAuditor(userDetails);
        auditoriaService.registrarAccion(
                auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1],
                AuditoriaService.ACCION_APROBAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Aprobar solicitud de registro: " + solicitud.getNombreCompleto() +
                " (" + solicitud.getCorreoCorporativo() + ")",
                null, null, "EXITOSO", null,
                HttpClientIpResolver.resolve(request), request.getHeader("User-Agent"));

        // Notificación in-app
        try { notificacionService.notificarAprobacion(guardado.getId()); } catch (Exception ex) { /* no rollback */ }

        // Email con credenciales
        try {
            emailService.enviarCredenciales(
                    guardado.getCorreoCorporativo(),
                    guardado.getNombreCompleto(),
                    guardado.getCorreoCorporativo(),
                    tempPassword);
        } catch (Exception ex) { /* no rollback */ }

        return ResponseEntity.ok(Map.of("success", true, "usuarioId", guardado.getId()));
    }

    // POST /api/solicitudes/{id}/rechazar?motivo=...
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        SolicitudesRegistro solicitud = solicitudRepo.findById(id)
                .orElse(null);
        if (solicitud == null)
            return ResponseEntity.notFound().build();
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE)
            return ResponseEntity.badRequest().body("La solicitud ya fue procesada");

        solicitud.setEstado(EstadoSolicitud.RECHAZADA);
        solicitud.setMotivoRechazo(motivo);
        solicitud.setFechaResolucion(Instant.now());
        if (userDetails != null) {
            usuarioService.findByCorreo(userDetails.getUsername())
                    .map(dto -> usuarioRepo.findById(dto.getId()).orElse(null))
                    .ifPresent(solicitud::setRevisadaPor);
        }
        solicitudRepo.save(solicitud);

        String[] auditor = resolverAuditor(userDetails);
        auditoriaService.registrarAccion(
                auditor[0] != null ? Long.parseLong(auditor[0]) : null, auditor[1],
                AuditoriaService.ACCION_RECHAZAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Rechazar solicitud de registro: " + solicitud.getNombreCompleto() +
                " (" + solicitud.getCorreoCorporativo() + ")",
                null, null, "EXITOSO", null,
                HttpClientIpResolver.resolve(request), request.getHeader("User-Agent"));

        try {
            emailService.enviarRechazo(
                    solicitud.getCorreoCorporativo(),
                    solicitud.getNombreCompleto(),
                    motivo != null && !motivo.isBlank() ? motivo : "No se proporcionó un motivo");
        } catch (Exception ex) { /* no rollback */ }

        return ResponseEntity.ok(Map.of("success", true));
    }

    // GET /api/solicitudes/{id}
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<SolicitudRegistroDTO> obtener(@PathVariable Long id) {
        return solicitudRepo.findById(id)
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private SolicitudRegistroDTO toDTO(SolicitudesRegistro s) {
        SolicitudRegistroDTO dto = new SolicitudRegistroDTO();
        dto.setId(s.getId());
        dto.setNombreCompleto(s.getNombreCompleto());
        dto.setDni(s.getDni());
        dto.setCorreoCorporativo(s.getCorreoCorporativo());
        dto.setEmpresaNombre(s.getEmpresaNombre());
        dto.setEstado(s.getEstado() != null ? s.getEstado().name() : null);
        dto.setMotivoRechazo(s.getMotivoRechazo());
        dto.setFechaResolucion(s.getFechaResolucion());
        dto.setRevisadaPorNombre(s.getRevisadaPor() != null ? s.getRevisadaPor().getNombreCompleto() : null);
        dto.setCreatedAt(s.getCreatedAt());
        return dto;
    }

    // ── helpers ─────────────────────────────────────────────────

    private String generarPassword(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++)
            sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        return sb.toString();
    }

    private String[] resolverAuditor(UserDetails userDetails) {
        if (userDetails == null) return new String[]{null, "Sistema"};
        var dto = usuarioService.findByCorreo(userDetails.getUsername()).orElse(null);
        if (dto == null) return new String[]{null, userDetails.getUsername()};
        return new String[]{String.valueOf(dto.getId()), dto.getNombreCompleto()};
    }
}

package org.reciclame.demo.controller;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionRestController {

    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public NotificacionRestController(NotificacionService notificacionService, UsuarioRepository usuarioRepository) {
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar(@AuthenticationPrincipal UserDetails user) {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuario.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(@AuthenticationPrincipal UserDetails user) {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(Map.of("count", notificacionService.contarNoLeidas(usuario.getId())));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        try {
            notificacionService.marcarComoLeida(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/leer-todas")
    public ResponseEntity<?> marcarTodasLeidas(@AuthenticationPrincipal UserDetails user) {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        notificacionService.marcarTodasComoLeidas(usuario.getId());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/todas")
    public ResponseEntity<?> eliminarTodas(@AuthenticationPrincipal UserDetails user) {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        notificacionService.eliminarTodas(usuario.getId());
        return ResponseEntity.noContent().build();
    }
}
package org.reciclame.demo.controller;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionRestController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public NotificacionRestController(NotificacionService notificacionService,
                                      UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.noContent().build();
        }
        return usuarioService.findByCorreo(user.getUsername())
                .map(u -> ResponseEntity.ok(notificacionService.listarPorUsuario(u.getId())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // El usuarioId se pasa al service a propósito: ahí se valida que la
    // notificación sea del usuario logueado antes de tocarla (si no, cualquiera
    // podría marcar/borrar notificaciones ajenas solo adivinando el id).
    @PostMapping("/{id}/leer")
    public ResponseEntity<Map<String, Object>> marcarLeida(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails user) {
        Long usuarioId = resolverUsuarioId(user);
        if (usuarioId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean ok = notificacionService.marcarComoLeida(id, usuarioId);
        if (!ok) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserDetails user) {
        Long usuarioId = resolverUsuarioId(user);
        if (usuarioId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean ok = notificacionService.eliminarNotificacion(id, usuarioId);
        if (!ok) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }

    private Long resolverUsuarioId(UserDetails user) {
        if (user == null) return null;
        return usuarioService.findByCorreo(user.getUsername()).map(u -> u.getId()).orElse(null);
    }
}

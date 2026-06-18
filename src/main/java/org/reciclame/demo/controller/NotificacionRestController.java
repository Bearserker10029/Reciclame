package org.reciclame.demo.controller;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints REST consumidos por el dropdown de notificaciones del topbar.
 *
 * <p>El JS del topbar (templates/navbar/topbar.html) hace fetch a
 * {@code /api/notificaciones} para listar y a
 * {@code /api/notificaciones/{id}/leer} para marcar como leída.
 * Ambos resuelven las notificaciones del usuario autenticado, sin importar
 * su rol (ADMINISTRADOR, SUPERADMIN, SOCIO, VISUALIZADOR, IMPERSONATOR).</p>
 */
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

    /**
     * Devuelve la lista de notificaciones activas del usuario autenticado.
     * Responde con 204 si el principal no se resuelve a un usuario válido
     * (caso borde: sesión rota), en vez de 500, para que el dropdown muestre
     * el estado vacío.
     */
    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.noContent().build();
        }
        return usuarioService.findByCorreo(user.getUsername())
                .map(u -> ResponseEntity.ok(notificacionService.listarPorUsuario(u.getId())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Marca una notificación como leída. Responde 404 si la notificación no
     * pertenece al usuario autenticado o no existe (evita fugas de IDs).
     */
    @PostMapping("/{id}/leer")
    public ResponseEntity<Map<String, Object>> marcarLeida(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Long usuarioId = usuarioService.findByCorreo(user.getUsername())
                .map(u -> u.getId())
                .orElse(null);
        if (usuarioId == null) {
            return ResponseEntity.status(401).build();
        }

        // Verifica pertenencia antes de marcar para no exponer IDs ajenos.
        NotificacionResponseDTO notif = notificacionService.listarPorUsuario(usuarioId).stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .orElse(null);
        if (notif == null) {
            return ResponseEntity.notFound().build();
        }

        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok(Map.of("ok", true, "id", id));
    }
}

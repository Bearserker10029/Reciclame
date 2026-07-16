package org.reciclame.demo.controller;

import org.reciclame.demo.model.ChatConversacion;
import org.reciclame.demo.model.ChatMensaje;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.CentrifugoService;
import org.reciclame.demo.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatTiempoRealRestController {

    private final ChatService chatService;
    private final UsuarioRepository usuarioRepo;
    private final CentrifugoService centrifugo;

    public ChatTiempoRealRestController(ChatService chatService,
                                       UsuarioRepository usuarioRepo,
                                       CentrifugoService centrifugo) {
        this.chatService = chatService;
        this.usuarioRepo = usuarioRepo;
        this.centrifugo = centrifugo;
    }

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> token(@AuthenticationPrincipal UserDetails ud) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of(
                "token", centrifugo.generarTokenConexion(actual.getId()),
                "canal", "chat#" + actual.getId(),
                "usuarioId", String.valueOf(actual.getId())
        ));
    }

    @GetMapping("/contactos")
    public ResponseEntity<List<Map<String, Object>>> contactos(@AuthenticationPrincipal UserDetails ud) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.contactosDisponibles(actual).stream()
                .map(this::toUsuarioMin).toList());
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<List<Map<String, Object>>> conversaciones(@AuthenticationPrincipal UserDetails ud) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(chatService.misConversaciones(actual.getId()).stream()
                .map(c -> {
                    Usuario otro = chatService.otroParticipante(c, actual.getId());
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("otro", toUsuarioMin(otro));
                    m.put("noLeidos", chatService.noLeidosEnConversacion(c.getId(), actual.getId()));
                    m.put("ultimoMensaje", c.getUltimoMensaje());
                    m.put("ultimoMensajeAt", c.getUltimoMensajeAt() != null ? c.getUltimoMensajeAt().toString() : null);
                    return m;
                }).toList());
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, Object>> iniciar(@AuthenticationPrincipal UserDetails ud,
                                                       @RequestParam Long otroId) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        ChatConversacion c = chatService.obtenerOCrear(actual, otroId);
        Usuario otro = chatService.otroParticipante(c, actual.getId());
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("otro", toUsuarioMin(otro));
        return ResponseEntity.ok(m);
    }

    @GetMapping("/{id}/mensajes")
    public ResponseEntity<List<Map<String, Object>>> historial(@AuthenticationPrincipal UserDetails ud,
                                                               @PathVariable Long id) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        chatService.marcarLeidos(id, actual.getId());
        return ResponseEntity.ok(chatService.historial(id, actual.getId()).stream()
                .map(m -> {
                    Map<String, Object> x = new HashMap<>();
                    x.put("id", m.getId());
                    x.put("remitenteId", m.getRemitente().getId());
                    x.put("contenido", m.getContenido());
                    x.put("leido", Boolean.TRUE.equals(m.getLeido()));
                    x.put("createdAt", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
                    return x;
                }).toList());
    }

    @PostMapping("/{id}/mensajes")
    public ResponseEntity<Map<String, Object>> enviar(@AuthenticationPrincipal UserDetails ud,
                                                      @PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        try {
            ChatMensaje msg = chatService.enviarMensaje(actual, id, body.get("contenido"));
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", msg.getId());
            resp.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeidos(@AuthenticationPrincipal UserDetails ud,
                                             @PathVariable Long id) {
        Usuario actual = usuarioActual(ud);
        if (actual == null) return ResponseEntity.status(401).build();
        chatService.marcarLeidos(id, actual.getId());
        return ResponseEntity.noContent().build();
    }

    private Usuario usuarioActual(UserDetails ud) {
        if (ud == null) return null;
        return usuarioRepo.findByCorreoCorporativo(ud.getUsername()).orElse(null);
    }

    private Map<String, Object> toUsuarioMin(Usuario u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("nombreCompleto", u.getNombreCompleto());
        m.put("correo", u.getCorreoCorporativo());
        m.put("rol", u.getRol() != null ? u.getRol().getNombre() : null);
        m.put("empresa", u.getEmpresa() != null ? u.getEmpresa().getRazonSocial() : null);
        m.put("fotoUrl", u.getFotoUrl());
        return m;
    }
}

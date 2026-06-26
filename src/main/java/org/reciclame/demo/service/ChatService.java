package org.reciclame.demo.service;

import org.reciclame.demo.model.ChatConversacion;
import org.reciclame.demo.model.ChatMensaje;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.ChatConversacionRepository;
import org.reciclame.demo.repository.ChatMensajeRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ChatService {

    /** SUPERADMIN no participa en el chat. */
    private static final Set<String> ROLES_PARTICIPAN = Set.of("ADMINISTRADOR", "SOCIO");

    private final ChatConversacionRepository conversacionRepo;
    private final ChatMensajeRepository mensajeRepo;
    private final UsuarioRepository usuarioRepo;
    private final CentrifugoService centrifugo;

    public ChatService(ChatConversacionRepository conversacionRepo,
                       ChatMensajeRepository mensajeRepo,
                       UsuarioRepository usuarioRepo,
                       CentrifugoService centrifugo) {
        this.conversacionRepo = conversacionRepo;
        this.mensajeRepo = mensajeRepo;
        this.usuarioRepo = usuarioRepo;
        this.centrifugo = centrifugo;
    }

    public List<Usuario> contactosDisponibles(Usuario actual) {
        String rolActual = actual.getRol() != null ? actual.getRol().getNombre() : "";
        return usuarioRepo.findAll().stream()
                .filter(u -> !u.getId().equals(actual.getId()))
                .filter(u -> u.getRol() != null && ROLES_PARTICIPAN.contains(u.getRol().getNombre()))
                .filter(u -> puedenChatear(rolActual, u.getRol().getNombre()))
                .toList();
    }

    private boolean puedenChatear(String rolA, String rolB) {
        // Socio↔Socio, Socio↔Admin, Admin↔Socio. Admin↔Admin permitido también.
        return ROLES_PARTICIPAN.contains(rolA) && ROLES_PARTICIPAN.contains(rolB);
    }

    public List<ChatConversacion> misConversaciones(Long usuarioId) {
        return conversacionRepo.findByUsuarioId(usuarioId);
    }

    public Usuario otroParticipante(ChatConversacion c, Long miId) {
        return c.getUsuarioA().getId().equals(miId) ? c.getUsuarioB() : c.getUsuarioA();
    }

    public long noLeidosEnConversacion(Long convId, Long usuarioId) {
        return mensajeRepo.contarNoLeidos(convId, usuarioId);
    }

    @Transactional
    public ChatConversacion obtenerOCrear(Usuario actual, Long otroId) {
        if (actual.getId().equals(otroId)) {
            throw new IllegalArgumentException("No puedes chatear contigo mismo");
        }
        Usuario otro = usuarioRepo.findById(otroId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario destino no existe"));

        Long uA = Math.min(actual.getId(), otro.getId());
        Long uB = Math.max(actual.getId(), otro.getId());

        return conversacionRepo.findEntreUsuarios(uA, uB).orElseGet(() -> {
            ChatConversacion c = new ChatConversacion();
            c.setUsuarioA(uA.equals(actual.getId()) ? actual : otro);
            c.setUsuarioB(uA.equals(actual.getId()) ? otro : actual);
            return conversacionRepo.save(c);
        });
    }

    public List<ChatMensaje> historial(Long convId, Long usuarioId) {
        return mensajeRepo.findByConversacionIdOrderByCreatedAtAsc(convId);
    }

    @Transactional
    public ChatMensaje enviarMensaje(Usuario remitente, Long convId, String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        ChatConversacion conv = conversacionRepo.findById(convId)
                .orElseThrow(() -> new IllegalArgumentException("Conversación no existe"));

        boolean esParticipante = conv.getUsuarioA().getId().equals(remitente.getId())
                || conv.getUsuarioB().getId().equals(remitente.getId());
        if (!esParticipante) {
            throw new IllegalArgumentException("No participas en esta conversación");
        }

        ChatMensaje msg = new ChatMensaje();
        msg.setConversacion(conv);
        msg.setRemitente(remitente);
        msg.setContenido(contenido.trim());
        msg.setLeido(false);
        msg = mensajeRepo.save(msg);

        conv.setUltimoMensaje(msg.getContenido().length() > 200
                ? msg.getContenido().substring(0, 200) : msg.getContenido());
        conv.setUltimoMensajeAt(Instant.now());
        conversacionRepo.save(conv);

        // Publicar a Centrifugo para ambos participantes (tiempo real)
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", "mensaje");
        payload.put("conversacionId", conv.getId());
        payload.put("mensajeId", msg.getId());
        payload.put("remitenteId", remitente.getId());
        payload.put("remitenteNombre", remitente.getNombreCompleto());
        payload.put("contenido", msg.getContenido());
        payload.put("createdAt", Instant.now().toString());

        Long otroId = otroParticipante(conv, remitente.getId()).getId();
        centrifugo.publicar("chat#" + otroId, payload);
        centrifugo.publicar("chat#" + remitente.getId(), payload);

        return msg;
    }

    @Transactional
    public int marcarLeidos(Long convId, Long usuarioId) {
        int actualizados = mensajeRepo.marcarComoLeidos(convId, usuarioId);
        if (actualizados > 0) {
            // Avisar al otro participante (el remitente) que sus mensajes fueron leídos
            ChatConversacion conv = conversacionRepo.findById(convId).orElse(null);
            if (conv != null) {
                Long otroId = conv.getUsuarioA().getId().equals(usuarioId)
                        ? conv.getUsuarioB().getId()
                        : conv.getUsuarioA().getId();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("tipo", "leido");
                payload.put("conversacionId", convId);
                payload.put("lectorId", usuarioId);
                centrifugo.publicar("chat#" + otroId, payload);
            }
        }
        return actualizados;
    }
}

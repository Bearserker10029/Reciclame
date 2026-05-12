package org.reciclame.demo.service;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.model.Notificacione;
import org.reciclame.demo.model.PreferenciasNotificacion;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.NotificacionRepository;
import org.reciclame.demo.repository.PreferenciasNotificacionRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final PreferenciasNotificacionRepository preferenciasRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               PreferenciasNotificacionRepository preferenciasRepository,
                               UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.preferenciasRepository = preferenciasRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── READ ──────────────────────────────────────────────────
    public List<NotificacionResponseDTO> listarPorUsuario(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioIdAndEstadoActivoTrueOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalseAndEstadoActivoTrue(usuarioId);
    }

    // ── CREAR / ENVIAR ────────────────────────────────────────
    @Transactional
    public void crearNotificacion(Long usuarioId, String titulo, String mensaje, String tipo) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));
        Notificacione n = new Notificacione();
        n.setUsuario(u);
        n.setTipo(tipo != null ? tipo : "MENSAJE_ADMIN");
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setEstadoActivo(true);
        n.setCreatedAt(Instant.now());
        notificacionRepository.save(n);
    }

    @Transactional
    public int enviarNotificacionGlobal(String titulo, String mensaje, String tipo) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) {
            Notificacione n = new Notificacione();
            n.setUsuario(u);
            n.setTipo(tipo != null ? tipo : "MENSAJE_ADMIN");
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setLeida(false);
            n.setEstadoActivo(true);
            n.setCreatedAt(Instant.now());
            notificacionRepository.save(n);
        }
        return usuarios.size();
    }

    // ── MARCAR COMO LEÍDA ─────────────────────────────────────
    public void marcarComoLeida(Long id) {
        Notificacione notif = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con id: " + id));
        notif.setLeida(true);
        notif.setFechaLectura(Instant.now());
        notificacionRepository.save(notif);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarNotificacion(Long id) {
        Notificacione notif = notificacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada con id: " + id));
        notif.setEstadoActivo(false);
        notificacionRepository.save(notif);
    }

    // ── PREFERENCIAS ──────────────────────────────────────────
    public PreferenciasNotificacion obtenerOCrearPreferencias(Long usuarioId) {
        return preferenciasRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    PreferenciasNotificacion p = new PreferenciasNotificacion();
                    p.setEmailHabilitado(true);
                    p.setPushHabilitado(false);
                    p.setSmsHabilitado(false);
                    p.setNotifNuevasNormativas(true);
                    p.setNotifNuevosEstudios(true);
                    p.setNotifCambiosSeguidos(true);
                    p.setNotifMensajesAdmin(true);
                    p.setNotifDigestoSemanal(false);
                    p.setFrecuenciaEmail("DIARIA");
                    return p;
                });
    }

    @Transactional
    public void guardarPreferencias(Long usuarioId, boolean notifNuevasNormativas,
                                    boolean notifNuevosEstudios, boolean notifCambiosSeguidos,
                                    boolean emailHabilitado, String frecuenciaEmail) {
        PreferenciasNotificacion prefs = preferenciasRepository.findByUsuarioId(usuarioId)
                .orElseGet(() -> {
                    PreferenciasNotificacion p = new PreferenciasNotificacion();
                    Usuario u = usuarioRepository.findById(usuarioId)
                            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioId));
                    p.setUsuario(u);
                    p.setCreatedAt(Instant.now());
                    p.setPushHabilitado(false);
                    p.setSmsHabilitado(false);
                    p.setNotifMensajesAdmin(true);
                    p.setNotifDigestoSemanal(false);
                    return p;
                });
        prefs.setNotifNuevasNormativas(notifNuevasNormativas);
        prefs.setNotifNuevosEstudios(notifNuevosEstudios);
        prefs.setNotifCambiosSeguidos(notifCambiosSeguidos);
        prefs.setEmailHabilitado(emailHabilitado);
        prefs.setFrecuenciaEmail(frecuenciaEmail);
        prefs.setUpdatedAt(Instant.now());
        preferenciasRepository.save(prefs);
    }

    // ── NOTIFICACIONES AUTOMÁTICAS ────────────────────────────

    @Transactional
    public void notificarNuevoEstudio(String titulo) {
        usuarioRepository.findAll().forEach(u -> {
            boolean enviar = preferenciasRepository.findByUsuarioId(u.getId())
                    .map(p -> Boolean.TRUE.equals(p.getNotifNuevosEstudios()))
                    .orElse(true);
            if (!enviar) return;
            Notificacione n = buildNotif(u, "NUEVO_ESTUDIO",
                    "Nuevo estudio disponible",
                    "Se ha publicado el estudio: " + titulo,
                    "/admin/estudios");
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void notificarNuevaNormativa(String titulo) {
        usuarioRepository.findAll().forEach(u -> {
            boolean enviar = preferenciasRepository.findByUsuarioId(u.getId())
                    .map(p -> Boolean.TRUE.equals(p.getNotifNuevasNormativas()))
                    .orElse(true);
            if (!enviar) return;
            Notificacione n = buildNotif(u, "NUEVA_NORMATIVA",
                    "Nueva normativa disponible",
                    "Se ha agregado al repositorio: " + titulo,
                    "/admin/normativo");
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void notificarAprobacion(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            Notificacione n = buildNotif(u, "APROBACION",
                    "¡Tu cuenta ha sido aprobada!",
                    "Tu solicitud de acceso a Recíclame fue aprobada. Ya puedes ingresar al repositorio.",
                    null);
            notificacionRepository.save(n);
        });
    }

    @Transactional
    public void notificarRechazo(Long usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            Notificacione n = buildNotif(u, "RECHAZO",
                    "Solicitud de acceso no aprobada",
                    "Tu solicitud de acceso fue revisada y no pudo ser aprobada en esta ocasión. Puedes contactar al administrador.",
                    null);
            notificacionRepository.save(n);
        });
    }

    private Notificacione buildNotif(Usuario u, String tipo, String titulo, String mensaje, String enlace) {
        Notificacione n = new Notificacione();
        n.setUsuario(u);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setEnlaceRecurso(enlace);
        n.setLeida(false);
        n.setEstadoActivo(true);
        n.setCreatedAt(Instant.now());
        return n;
    }

    // ── HELPER ────────────────────────────────────────────────
    private NotificacionResponseDTO toResponseDTO(Notificacione n) {
        NotificacionResponseDTO dto = new NotificacionResponseDTO();
        dto.setId(n.getId());
        dto.setTipo(n.getTipo());
        dto.setTitulo(n.getTitulo());
        dto.setMensaje(n.getMensaje());
        dto.setEnlaceRecurso(n.getEnlaceRecurso());
        dto.setLeida(n.getLeida());
        dto.setCreatedAt(n.getCreatedAt());
        dto.setTiempoRelativo(calcularTiempoRelativo(n.getCreatedAt()));
        return dto;
    }

    private String calcularTiempoRelativo(Instant fecha) {
        long minutos = ChronoUnit.MINUTES.between(fecha, Instant.now());
        if (minutos < 60)   return "hace " + minutos + " min";
        long horas = ChronoUnit.HOURS.between(fecha, Instant.now());
        if (horas < 24)     return "hace " + horas + " hora" + (horas > 1 ? "s" : "");
        long dias = ChronoUnit.DAYS.between(fecha, Instant.now());
        if (dias < 7)       return "hace " + dias + " día" + (dias > 1 ? "s" : "");
        long semanas = dias / 7;
        if (semanas < 4)    return "hace " + semanas + " semana" + (semanas > 1 ? "s" : "");
        long meses = dias / 30;
        return "hace " + meses + " mes" + (meses > 1 ? "es" : "");
    }
}

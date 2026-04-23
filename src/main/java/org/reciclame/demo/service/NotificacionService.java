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

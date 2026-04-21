package org.reciclame.demo.service;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.model.Notificacione;
import org.reciclame.demo.repository.NotificacionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
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

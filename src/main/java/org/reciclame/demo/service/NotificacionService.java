package org.reciclame.demo.service;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.model.Notificacione;
import org.reciclame.demo.model.PreferenciasNotificacion;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.NotificacionRepository;
import org.reciclame.demo.repository.PreferenciasNotificacionRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    // Tope de notificaciones traídas por el dropdown/lista; evita que la consulta
    // crezca sin límite con el historial completo del usuario.
    private static final int LIMITE_LISTADO = 50;

    private final NotificacionRepository notificacionRepository;
    private final PreferenciasNotificacionRepository preferenciasRepository;
    private final UsuarioRepository usuarioRepository;
    private final CentrifugoService centrifugoService;
    private final EmailNotificationService emailNotificationService;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               PreferenciasNotificacionRepository preferenciasRepository,
                               UsuarioRepository usuarioRepository,
                               CentrifugoService centrifugoService,
                               EmailNotificationService emailNotificationService) {
        this.notificacionRepository = notificacionRepository;
        this.preferenciasRepository = preferenciasRepository;
        this.usuarioRepository = usuarioRepository;
        this.centrifugoService = centrifugoService;
        this.emailNotificationService = emailNotificationService;
    }

    // ── READ ──────────────────────────────────────────────────
    public List<NotificacionResponseDTO> listarPorUsuario(Long usuarioId) {
        return notificacionRepository
                .findByUsuarioIdAndEstadoActivoTrueOrderByCreatedAtDesc(usuarioId, PageRequest.of(0, LIMITE_LISTADO))
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
        Notificacione n = buildNotif(u, tipo != null ? tipo : "MENSAJE_ADMIN", titulo, mensaje, null);
        notificacionRepository.save(n);
        pushNotif(u.getId(), titulo, mensaje, n.getTipo(), null);
        enviarEmailSiHabilitado(u, titulo, mensaje);
    }

    @Transactional
    public int enviarNotificacionGlobal(String titulo, String mensaje, String tipo) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        int enviados = 0;
        for (Usuario u : usuarios) {
            // A diferencia de esta, el resto de notificadores (notificarNuevoEstudio,
            // etc.) respetan la preferencia correspondiente antes de crear la
            // notificación in-app/push. Este broadcast manual no lo hacía: un
            // usuario que desactivó "mensajes de admin" igual recibía el mensaje
            // en la campanita y el push cada vez que un SuperAdmin mandaba uno.
            boolean enviar = preferenciasRepository.findByUsuarioId(u.getId())
                    .map(p -> Boolean.TRUE.equals(p.getNotifMensajesAdmin()))
                    .orElse(true);
            if (!enviar) continue;
            Notificacione n = buildNotif(u, tipo != null ? tipo : "MENSAJE_ADMIN", titulo, mensaje, null);
            notificacionRepository.save(n);
            pushNotif(u.getId(), titulo, mensaje, n.getTipo(), null);
            enviarEmailSiHabilitado(u, titulo, mensaje);
            enviados++;
        }
        return enviados;
    }

    // ── MARCAR COMO LEÍDA ─────────────────────────────────────
    /**
     * Marca como leída solo si la notificación pertenece a {@code usuarioId}.
     * Evita que un usuario marque (o descubra la existencia de) notificaciones ajenas.
     */
    @Transactional
    public boolean marcarComoLeida(Long id, Long usuarioId) {
        Notificacione notif = notificacionRepository.findById(id).orElse(null);
        if (notif == null || !notif.getUsuario().getId().equals(usuarioId)) {
            return false;
        }
        notif.setLeida(true);
        notif.setFechaLectura(Instant.now());
        notificacionRepository.save(notif);
        return true;
    }

    // ── DELETE lógico ─────────────────────────────────────────
    /**
     * Igual que {@link #marcarComoLeida}: solo borra (lógicamente) si la
     * notificación pertenece al usuario que la solicita.
     */
    @Transactional
    public boolean eliminarNotificacion(Long id, Long usuarioId) {
        Notificacione notif = notificacionRepository.findById(id).orElse(null);
        if (notif == null || !notif.getUsuario().getId().equals(usuarioId)) {
            return false;
        }
        notif.setEstadoActivo(false);
        notificacionRepository.save(notif);
        return true;
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
            pushNotif(u.getId(), "Nuevo estudio disponible", "Se ha publicado: " + titulo, "NUEVO_ESTUDIO", "/socio/estudios");
            enviarEmailSiHabilitado(u, "Nuevo estudio disponible", "Se ha publicado el estudio: " + titulo);
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
            pushNotif(u.getId(), "Nueva normativa disponible", "Se agregó: " + titulo, "NUEVA_NORMATIVA", "/socio/normativo");
            enviarEmailSiHabilitado(u, "Nueva normativa disponible", "Se ha agregado al repositorio: " + titulo);
        });
    }

    @Transactional
    public void notificarEstudioActualizado(String titulo) {
        usuarioRepository.findAll().forEach(u -> {
            boolean enviar = preferenciasRepository.findByUsuarioId(u.getId())
                    .map(p -> Boolean.TRUE.equals(p.getNotifNuevosEstudios()))
                    .orElse(true);
            if (!enviar) return;
            Notificacione n = buildNotif(u, "NUEVO_ESTUDIO",
                    "Estudio actualizado",
                    "Se ha actualizado el estudio: " + titulo,
                    "/admin/estudios");
            notificacionRepository.save(n);
            pushNotif(u.getId(), "Estudio actualizado", "Se actualizó: " + titulo, "NUEVO_ESTUDIO", "/socio/estudios");
            enviarEmailSiHabilitado(u, "Estudio actualizado", "Se ha actualizado el estudio: " + titulo);
        });
    }

    @Transactional
    public void notificarNormativaActualizada(String titulo) {
        usuarioRepository.findAll().forEach(u -> {
            boolean enviar = preferenciasRepository.findByUsuarioId(u.getId())
                    .map(p -> Boolean.TRUE.equals(p.getNotifNuevasNormativas()))
                    .orElse(true);
            if (!enviar) return;
            Notificacione n = buildNotif(u, "NUEVA_NORMATIVA",
                    "Normativa actualizada",
                    "Se ha actualizado en el repositorio: " + titulo,
                    "/admin/normativo");
            notificacionRepository.save(n);
            pushNotif(u.getId(), "Normativa actualizada", "Se actualizó: " + titulo, "NUEVA_NORMATIVA", "/socio/normativo");
            enviarEmailSiHabilitado(u, "Normativa actualizada", "Se ha actualizado en el repositorio: " + titulo);
        });
    }

    @Transactional
    public void notificarDocumentoCompartido(Usuario destinatario, String compartidoPorNombre,
                                             String titulo, String enlace) {
        String mensaje = compartidoPorNombre + " te dio acceso a: " + titulo;
        Notificacione n = buildNotif(destinatario, "DOCUMENTO_COMPARTIDO",
                "Te compartieron un documento", mensaje, enlace);
        notificacionRepository.save(n);
        pushNotif(destinatario.getId(), "Te compartieron un documento", mensaje, "DOCUMENTO_COMPARTIDO", enlace);
        enviarEmailSiHabilitado(destinatario, "Te compartieron un documento", mensaje);
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

    private void pushNotif(Long usuarioId, String titulo, String mensaje, String tipo, String enlace) {
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("tipo", "notificacion");
            payload.put("notifTipo", tipo);
            payload.put("titulo", titulo);
            payload.put("mensaje", mensaje);
            payload.put("enlace", enlace != null ? enlace : "");
            centrifugoService.publicar("chat#" + usuarioId, payload);
        } catch (Exception ex) { /* no rollback */ }
    }

    /**
     * Envía copia por correo si el usuario tiene emailHabilitado=true y
     * frecuenciaEmail != 'NUNCA'. No implementa digesto real (agrupar por
     * DIARIA/SEMANAL requeriría un job programado); por ahora cualquier
     * frecuencia distinta de NUNCA envía de inmediato, igual que INMEDIATA.
     */
    private void enviarEmailSiHabilitado(Usuario u, String titulo, String mensaje) {
        try {
            if (u.getCorreoCorporativo() == null) return;
            PreferenciasNotificacion prefs = preferenciasRepository.findByUsuarioId(u.getId()).orElse(null);
            boolean habilitado = prefs == null || Boolean.TRUE.equals(prefs.getEmailHabilitado());
            boolean nunca = prefs != null && "NUNCA".equalsIgnoreCase(prefs.getFrecuenciaEmail());
            if (!habilitado || nunca) return;
            emailNotificationService.sendEmail(u.getCorreoCorporativo(), titulo,
                    "<p>" + mensaje + "</p><p style=\"color:#9CA3AF;font-size:12px;\">Recíclame — puedes desactivar estos avisos en Configuración de notificaciones.</p>");
        } catch (Exception ex) { /* no rollback */ }
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

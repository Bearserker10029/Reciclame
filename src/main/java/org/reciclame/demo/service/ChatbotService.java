package org.reciclame.demo.service;

import jakarta.servlet.http.HttpServletRequest;
import org.reciclame.demo.dto.ChatbotConsultaFilaDTO;
import org.reciclame.demo.dto.ChatbotEstadisticasDTO;
import org.reciclame.demo.dto.ChatbotRegistroRequestDTO;
import org.reciclame.demo.dto.ChatbotRegistroResponseDTO;
import org.reciclame.demo.model.ConversacionesChatbot;
import org.reciclame.demo.model.MensajesChatbot;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.ChatbotFeedbackRepository;
import org.reciclame.demo.repository.ConversacionesChatbotRepository;
import org.reciclame.demo.repository.MensajesChatbotRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio para el chatbot Reciclame.
 * <p>
 * - Registra conversaciones y mensajes a partir del callback del frontend.
 * - Calcula KPIs y series de la pestana "Chatbot Analytics" de reportes.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final DateTimeFormatter FECHA_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/Lima"));

    private final ConversacionesChatbotRepository conversacionRepo;
    private final MensajesChatbotRepository mensajeRepo;
    private final ChatbotFeedbackRepository feedbackRepo;
    private final UsuarioRepository usuarioRepo;

    public ChatbotService(ConversacionesChatbotRepository conversacionRepo,
                         MensajesChatbotRepository mensajeRepo,
                         ChatbotFeedbackRepository feedbackRepo,
                         UsuarioRepository usuarioRepo) {
        this.conversacionRepo = conversacionRepo;
        this.mensajeRepo = mensajeRepo;
        this.feedbackRepo = feedbackRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // ====================================================================
    // 1) REGISTRO (callback)
    // ====================================================================

    @Transactional
    public ChatbotRegistroResponseDTO registrarConversacion(ChatbotRegistroRequestDTO req,
                                                            HttpServletRequest httpReq) {
        if (req == null || req.getPregunta() == null || req.getPregunta().isBlank()) {
            throw new IllegalArgumentException("La pregunta del usuario es obligatoria");
        }
        Optional<Usuario> usuarioOpt = usuarioRepo.findById(req.getUsuarioId());
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado: " + req.getUsuarioId());
        }
        Usuario usuario = usuarioOpt.get();

        List<String> warnings = new ArrayList<>();

        // 1) Crear conversacion
        ConversacionesChatbot conv = new ConversacionesChatbot();
        conv.setUsuario(usuario);
        conv.setIdioma(Optional.ofNullable(req.getIdioma()).orElse("es"));
        conv.setTitulo(truncar(req.getPregunta(), 200));
        conv.setResumen(req.getPregunta());
        conv.setTotalMensajes(0);
        conv.setDuracionTotalMs(0L);
        conv.setUltimoMensajeAt(Instant.now());
        conv.setIpAddress(firstNonBlank(req.getIpAddress(), clientIp(httpReq)));
        conv.setUserAgent(truncar(req.getUserAgent(), 500));
        conv.setCerradaPorUsuario(false);
        conv.setEstadoActivo(true);
        Instant now = Instant.now();
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conv = conversacionRepo.save(conv);

        // 2) Registrar el mensaje del usuario
        MensajesChatbot msgUser = new MensajesChatbot();
        msgUser.setConversacion(conv);
        msgUser.setRol("USER");
        msgUser.setContenido(req.getPregunta());
        msgUser.setIntentDetectado(req.getIntentDetectado());
        if (req.getConfianza() != null) {
            msgUser.setConfianza(BigDecimal.valueOf(req.getConfianza())
                    .setScale(4, RoundingMode.HALF_UP));
        }
        msgUser.setDerivadoSoporte(Boolean.TRUE.equals(req.getDerivadoSoporte()));
        msgUser.setDuracionMs(null); // el del usuario no aplica
        msgUser.setTokensEstimados(req.getTokensEstimados());
        msgUser.setModelo(req.getModelo());
        msgUser.setEsError(false);
        msgUser.setSqlEjecutado(truncar(req.getSqlEjecutado(), 4000));
        msgUser.setFilasDevueltas(req.getFilasDevueltas());
        msgUser.setMetadata(truncar(req.getMetadata(), 4000));
        msgUser.setIpAddress(conv.getIpAddress());
        msgUser.setCreatedAt(now);
        msgUser = mensajeRepo.save(msgUser);

        // 3) Registrar la respuesta del bot
        MensajesChatbot msgBot = null;
        if (req.getRespuesta() != null && !req.getRespuesta().isBlank()) {
            msgBot = new MensajesChatbot();
            msgBot.setConversacion(conv);
            msgBot.setRol("BOT");
            msgBot.setContenido(req.getRespuesta());
            msgBot.setDuracionMs(req.getDuracionMs());
            msgBot.setTokensEstimados(req.getTokensEstimados());
            msgBot.setModelo(req.getModelo());
            msgBot.setEsError(Boolean.TRUE.equals(req.getEsError()));
            msgBot.setMensajeError(truncar(req.getMensajeError(), 500));
            msgBot.setSqlEjecutado(truncar(req.getSqlEjecutado(), 4000));
            msgBot.setFilasDevueltas(req.getFilasDevueltas());
            msgBot.setMetadata(truncar(req.getMetadata(), 4000));
            msgBot.setIpAddress(conv.getIpAddress());
            msgBot.setCreatedAt(Instant.now());
            msgBot = mensajeRepo.save(msgBot);
        } else {
            warnings.add("Respuesta del bot vacia; no se registro el mensaje BOT");
        }

        // 4) Actualizar totales en la conversacion
        int totalMsg = 1 + (msgBot != null ? 1 : 0);
        conv.setTotalMensajes(totalMsg);
        conv.setUltimoMensajeAt(Instant.now());
        if (req.getDuracionMs() != null) {
            conv.setDuracionTotalMs(req.getDuracionMs());
        }
        conv.setUpdatedAt(Instant.now());
        conversacionRepo.save(conv);

        return new ChatbotRegistroResponseDTO(
                conv.getId(),
                msgUser.getId(),
                msgBot != null ? msgBot.getId() : null,
                conv.getCreatedAt(),
                warnings
        );
    }

    // ====================================================================
    // 2) ESTADISTICAS (para reportes Admin / SuperAdmin)
    // ====================================================================

    @Transactional(readOnly = true)
    public ChatbotEstadisticasDTO calcularEstadisticas(Instant desde, Instant hasta) {
        ChatbotEstadisticasDTO dto = new ChatbotEstadisticasDTO();
        dto.setTotalConversaciones(conversacionRepo.countByRango(desde, hasta));
        dto.setTotalMensajes(mensajeRepo.countByRango(desde, hasta));
        dto.setTotalMensajesUsuario(mensajeRepo.countUserByRango(desde, hasta));
        dto.setTotalMensajesBot(mensajeRepo.countBotByRango(desde, hasta));
        dto.setTotalErrores(mensajeRepo.countErroresByRango(desde, hasta));
        dto.setTotalDerivadosSoporte(mensajeRepo.countDerivadosByRango(desde, hasta));
        dto.setUsuariosUnicos(conversacionRepo.countDistinctUsuariosByRango(desde, hasta));
        Double avg = mensajeRepo.avgLatenciaBot(desde, hasta);
        dto.setLatenciaPromedioMs(avg != null ? Math.round(avg) : 0d);
        dto.setFeedbackPositivo(feedbackRepo.countPositivo(desde, hasta));
        dto.setFeedbackNegativo(feedbackRepo.countNegativo(desde, hasta));

        // Series para charts
        dto.setMensajesPorDia(agruparMensajesPorDia(desde, hasta));
        dto.setConversacionesPorRol(agruparPorRol(desde, hasta));
        dto.setTopIntents(obtenerTopIntents(desde, hasta));
        dto.setTopUsuarios(obtenerTopUsuarios(desde, hasta));
        return dto;
    }

    // ====================================================================
    // 3) LISTADO FILTRADO (tabla de conversaciones)
    // ====================================================================

    @Transactional(readOnly = true)
    public Page<ChatbotConsultaFilaDTO> listarConversacionesFiltrado(Long usuarioId,
                                                                     Long rolId,
                                                                     Instant desde,
                                                                     Instant hasta,
                                                                     Pageable pageable) {
        Page<ConversacionesChatbot> page = conversacionRepo.filtrar(usuarioId, rolId, desde, hasta, pageable);
        return page.map(this::toFila);
    }

    @Transactional(readOnly = true)
    public List<ChatbotConsultaFilaDTO.MensajeDetalleDTO> obtenerDetalleConversacion(Long conversacionId) {
        List<MensajesChatbot> mensajes = mensajeRepo.findByConversacion_IdOrderByCreatedAtAsc(conversacionId);
        List<ChatbotConsultaFilaDTO.MensajeDetalleDTO> out = new ArrayList<>();
        for (MensajesChatbot m : mensajes) {
            ChatbotConsultaFilaDTO.MensajeDetalleDTO dto = new ChatbotConsultaFilaDTO.MensajeDetalleDTO();
            dto.setId(m.getId());
            dto.setRol(m.getRol());
            dto.setContenido(m.getContenido());
            dto.setIntentDetectado(m.getIntentDetectado());
            dto.setDuracionMs(m.getDuracionMs());
            dto.setTokensEstimados(m.getTokensEstimados());
            dto.setEsError(m.getEsError());
            dto.setCreatedAt(m.getCreatedAt());
            out.add(dto);
        }
        return out;
    }

    // ====================================================================
    // 4) HELPERS
    // ====================================================================

    private ChatbotConsultaFilaDTO toFila(ConversacionesChatbot c) {
        ChatbotConsultaFilaDTO dto = new ChatbotConsultaFilaDTO();
        dto.setConversacionId(c.getId());
        dto.setFechaInicio(c.getCreatedAt());
        dto.setFechaUltimoMensaje(c.getUltimoMensajeAt() != null ? c.getUltimoMensajeAt() : c.getCreatedAt());
        if (c.getUsuario() != null) {
            dto.setUsuarioId(c.getUsuario().getId());
            dto.setUsuarioNombre(c.getUsuario().getNombreCompleto());
            dto.setUsuarioCorreo(c.getUsuario().getCorreoCorporativo());
            dto.setUsuarioRol(c.getUsuario().getRol() != null ? c.getUsuario().getRol().getNombre() : "N/A");
            dto.setUsuarioEmpresa(c.getUsuario().getEmpresa() != null ? c.getUsuario().getEmpresa().getRazonSocial() : null);
        }
        dto.setTotalMensajes(c.getTotalMensajes());
        dto.setDuracionTotalMs(c.getDuracionTotalMs());
        dto.setIdioma(c.getIdioma());
        dto.setDerivadoSoporte(Boolean.FALSE);

        List<MensajesChatbot> mensajes = mensajeRepo.findByConversacion_IdOrderByCreatedAtAsc(c.getId());
        int errores = 0;
        boolean derivado = false;
        MensajesChatbot primeraUser = null;
        MensajesChatbot ultimaBot = null;
        for (MensajesChatbot m : mensajes) {
            if (Boolean.TRUE.equals(m.getEsError())) errores++;
            if (Boolean.TRUE.equals(m.getDerivadoSoporte())) derivado = true;
            if ("USER".equals(m.getRol()) && primeraUser == null) primeraUser = m;
            if ("BOT".equals(m.getRol())) ultimaBot = m;
        }
        dto.setErrores(errores);
        dto.setDerivadoSoporte(derivado);
        if (primeraUser != null) dto.setPrimeraPregunta(primeraUser.getContenido());
        if (ultimaBot != null) dto.setUltimaRespuesta(ultimaBot.getContenido());
        return dto;
    }

    private List<Map<String, Object>> agruparMensajesPorDia(Instant desde, Instant hasta) {
        List<Object[]> rows = mensajeRepo.mensajesPorDia(desde, hasta);
        Map<String, long[]> bucket = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String fecha = Objects.toString(r[0], FECHA_FMT.format(Instant.now()));
            String rol = Objects.toString(r[1], "USER");
            long total = ((Number) r[2]).longValue();
            bucket.computeIfAbsent(fecha, k -> new long[2]);
            if ("BOT".equals(rol)) bucket.get(fecha)[0] += total;
            else bucket.get(fecha)[1] += total;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : bucket.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fecha", e.getKey());
            row.put("bot", e.getValue()[0]);
            row.put("user", e.getValue()[1]);
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> agruparPorRol(Instant desde, Instant hasta) {
        List<Object[]> rows = conversacionRepo.conversacionesPorRol(desde, hasta);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rol", r[0]);
            row.put("total", ((Number) r[1]).longValue());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> obtenerTopIntents(Instant desde, Instant hasta) {
        List<Object[]> rows = mensajeRepo.topIntents(desde, hasta, PageRequest.of(0, 5));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("intent", r[0]);
            row.put("total", ((Number) r[1]).longValue());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> obtenerTopUsuarios(Instant desde, Instant hasta) {
        List<Object[]> rows = conversacionRepo.topUsuarios(desde, hasta, PageRequest.of(0, 5));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("usuarioId", r[0]);
            row.put("nombre", r[1]);
            row.put("total", ((Number) r[2]).longValue());
            out.add(row);
        }
        return out;
    }

    private static String truncar(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String clientIp(HttpServletRequest req) {
        if (req == null) return null;
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
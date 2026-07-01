package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.reciclame.demo.dto.ChatbotConsultaFilaDTO;
import org.reciclame.demo.dto.ChatbotEstadisticasDTO;
import org.reciclame.demo.dto.ChatbotRegistroRequestDTO;
import org.reciclame.demo.dto.ChatbotRegistroResponseDTO;
import org.reciclame.demo.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints REST para el chatbot Reciclame.
 * <p>
 * - POST /api/chatbot/registrar     -> callback que el frontend hace al recibir la respuesta del webhook n8n.
 *                                      Guardado en conversaciones_chatbot + mensajes_chatbot.
 *                                      Requiere sesion del usuario (la consulta la esta haciendo el propio usuario).
 * - GET  /api/chatbot/estadisticas  -> KPIs y series para el dashboard Admin/SuperAdmin.
 * - GET  /api/chatbot/conversaciones -> listado paginado y filtrable para el dashboard.
 * - GET  /api/chatbot/conversaciones/{id}/mensajes -> detalle de una conversacion.
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotEstadisticasRestController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotEstadisticasRestController.class);

    private final ChatbotService chatbotService;

    public ChatbotEstadisticasRestController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // ----------------------------------------------------------------
    // 1) Callback del frontend (registro de pregunta + respuesta)
    // ----------------------------------------------------------------
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody ChatbotRegistroRequestDTO body,
                                       HttpServletRequest req,
                                       Authentication auth) {
        // Si el usuario autenticado es distinto al del body, usamos el del body
        // (el chatbot puede ser llamado desde un visor como el Admin revisando un chat)
        // pero solo permitimos que un usuario con rol registrable (no anonimo) lo envie.
        try {
            ChatbotRegistroResponseDTO out = chatbotService.registrarConversacion(body, req);
            return ResponseEntity.ok(out);
        } catch (IllegalArgumentException ex) {
            log.warn("Chatbot callback rechazo: {}", ex.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        } catch (Exception ex) {
            log.error("Error registrando conversacion chatbot", ex);
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "No se pudo registrar la conversacion: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    // ----------------------------------------------------------------
    // 2) KPIs / charts para Admin y SuperAdmin
    // ----------------------------------------------------------------
    @GetMapping("/estadisticas")
    public ResponseEntity<ChatbotEstadisticasDTO> estadisticas(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Authentication auth) {
        Instant desdeI = desde != null ? desde.atStartOfDay(ZoneId.of("America/Lima")).toInstant() : null;
        Instant hastaI = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant().minusMillis(1) : null;
        boolean excluirSuperadmin = esAdministrador(auth);
        return ResponseEntity.ok(chatbotService.calcularEstadisticas(desdeI, hastaI, excluirSuperadmin));
    }

    // ----------------------------------------------------------------
    // 3) Listado paginado y filtrado
    // ----------------------------------------------------------------
    @GetMapping("/conversaciones")
    public ResponseEntity<Map<String, Object>> listarConversaciones(
            @RequestParam(value = "usuarioId", required = false) Long usuarioId,
            @RequestParam(value = "rolId",     required = false) Long rolId,
            @RequestParam(value = "desde",     required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta",     required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(value = "q",         required = false) String q,
            @RequestParam(value = "page",      defaultValue = "0") int page,
            @RequestParam(value = "size",      defaultValue = "20") int size,
            Authentication auth) {

        Instant desdeI = desde != null ? desde.atStartOfDay(ZoneId.of("America/Lima")).toInstant() : null;
        Instant hastaI = hasta != null ? hasta.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant().minusMillis(1) : null;
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        boolean excluirSuperadmin = esAdministrador(auth);
        Page<ChatbotConsultaFilaDTO> result = chatbotService.listarConversacionesFiltrado(
                usuarioId, rolId, desdeI, hastaI, pageable, excluirSuperadmin);

        Map<String, Object> resp = new HashMap<>();
        resp.put("content", result.getContent());
        resp.put("page", result.getNumber());
        resp.put("size", result.getSize());
        resp.put("totalElements", result.getTotalElements());
        resp.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(resp);
    }

    /**
     * Devuelve true si el solicitante NO es superadmin (es decir, es admin/socio/visualizador,
     * o no hay auth). La política es: solo SUPERADMIN ve TODO; el resto oculta lo del superadmin.
     */
    private boolean esAdministrador(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return true;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.toUpperCase().contains("SUPERADMIN")) return false;
        }
        return true;
    }

    // ----------------------------------------------------------------
    // 4) Detalle de mensajes por conversacion
    // ----------------------------------------------------------------
    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<List<ChatbotConsultaFilaDTO.MensajeDetalleDTO>> mensajes(
            @PathVariable("id") Long conversacionId) {
        return ResponseEntity.ok(chatbotService.obtenerDetalleConversacion(conversacionId));
    }
}
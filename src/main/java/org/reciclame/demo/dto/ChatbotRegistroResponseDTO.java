package org.reciclame.demo.dto;

import java.time.Instant;
import java.util.List;

/**
 * Respuesta del backend tras un callback de registro.
 */
public class ChatbotRegistroResponseDTO {

    private Long conversacionId;
    private Long mensajeUsuarioId;
    private Long mensajeBotId;
    private Instant createdAt;
    private List<String> warnings;

    public ChatbotRegistroResponseDTO() {}

    public ChatbotRegistroResponseDTO(Long conversacionId, Long mensajeUsuarioId, Long mensajeBotId,
                                       Instant createdAt, List<String> warnings) {
        this.conversacionId = conversacionId;
        this.mensajeUsuarioId = mensajeUsuarioId;
        this.mensajeBotId = mensajeBotId;
        this.createdAt = createdAt;
        this.warnings = warnings;
    }

    public Long getConversacionId() { return conversacionId; }
    public void setConversacionId(Long conversacionId) { this.conversacionId = conversacionId; }
    public Long getMensajeUsuarioId() { return mensajeUsuarioId; }
    public void setMensajeUsuarioId(Long mensajeUsuarioId) { this.mensajeUsuarioId = mensajeUsuarioId; }
    public Long getMensajeBotId() { return mensajeBotId; }
    public void setMensajeBotId(Long mensajeBotId) { this.mensajeBotId = mensajeBotId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
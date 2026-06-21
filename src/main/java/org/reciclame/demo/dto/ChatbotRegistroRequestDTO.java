package org.reciclame.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Payload que envia el frontend (componentes/chatbot.html) al backend para
 * registrar una conversacion completa (pregunta + respuesta) una vez que n8n
 * respondio. El frontend sigue llamando directo al webhook n8n, y DESPUES hace
 * este callback.
 */
public class ChatbotRegistroRequestDTO {

    @NotNull
    private Long usuarioId;

    @NotBlank
    private String usuarioRol;

    @NotBlank
    private String usuarioNombre;

    /** Idioma del cliente (es, en) */
    private String idioma = "es";

    /** Texto enviado al webhook (pregunta del usuario) */
    @NotBlank
    private String pregunta;

    /** Texto devuelto por el webhook n8n (respuesta sanitizada) */
    private String respuesta;

    /** Latencia medida en el cliente (ms) entre envio y respuesta */
    private Long duracionMs;

    /** Modelo LLM si se conoce (ej: minimax, gpt-4o-mini) */
    private String modelo;

    /** Tokens estimados del intercambio (entrada + salida) */
    private Integer tokensEstimados;

    /** Intent detectado por el agente (opcional) */
    private String intentDetectado;

    /** Score de confianza 0..1 (opcional) */
    private Double confianza;

    /** true si el agente decidio escalar a soporte humano */
    private Boolean derivadoSoporte = false;

    /** true si la respuesta fue un error (timeout, excepcion, etc.) */
    private Boolean esError = false;

    /** Mensaje de error si esError=true */
    private String mensajeError;

    /** SQL ejecutado por la tool SQL del agente (opcional, truncado) */
    private String sqlEjecutado;

    /** Filas devueltas por la tool SQL */
    private Integer filasDevueltas;

    /** Metadata libre serializada como JSON */
    private String metadata;

    /** IP del cliente (opcional, normalmente lo determina el controller via request) */
    private String ipAddress;

    /** User-Agent (idem) */
    private String userAgent;

    /** Lista de pares user/bot si se quiere registrar la conversacion completa de una sola vez */
    private List<MensajePares> mensajes;

    public static class MensajePares {
        private String pregunta;
        private String respuesta;
        private Long duracionMs;

        public String getPregunta() { return pregunta; }
        public void setPregunta(String pregunta) { this.pregunta = pregunta; }
        public String getRespuesta() { return respuesta; }
        public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
        public Long getDuracionMs() { return duracionMs; }
        public void setDuracionMs(Long duracionMs) { this.duracionMs = duracionMs; }
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String usuarioRol) { this.usuarioRol = usuarioRol; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    public String getPregunta() { return pregunta; }
    public void setPregunta(String pregunta) { this.pregunta = pregunta; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public Long getDuracionMs() { return duracionMs; }
    public void setDuracionMs(Long duracionMs) { this.duracionMs = duracionMs; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getTokensEstimados() { return tokensEstimados; }
    public void setTokensEstimados(Integer tokensEstimados) { this.tokensEstimados = tokensEstimados; }
    public String getIntentDetectado() { return intentDetectado; }
    public void setIntentDetectado(String intentDetectado) { this.intentDetectado = intentDetectado; }
    public Double getConfianza() { return confianza; }
    public void setConfianza(Double confianza) { this.confianza = confianza; }
    public Boolean getDerivadoSoporte() { return derivadoSoporte; }
    public void setDerivadoSoporte(Boolean derivadoSoporte) { this.derivadoSoporte = derivadoSoporte; }
    public Boolean getEsError() { return esError; }
    public void setEsError(Boolean esError) { this.esError = esError; }
    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
    public String getSqlEjecutado() { return sqlEjecutado; }
    public void setSqlEjecutado(String sqlEjecutado) { this.sqlEjecutado = sqlEjecutado; }
    public Integer getFilasDevueltas() { return filasDevueltas; }
    public void setFilasDevueltas(Integer filasDevueltas) { this.filasDevueltas = filasDevueltas; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public List<MensajePares> getMensajes() { return mensajes; }
    public void setMensajes(List<MensajePares> mensajes) { this.mensajes = mensajes; }
}
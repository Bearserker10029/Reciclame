package org.reciclame.demo.dto;

import java.util.List;
import java.util.Map;

/**
 * KPIs y series graficables que consume la UI "Chatbot Analytics"
 * dentro de Admin/reportes.html y SuperAdmin/reportes.html.
 */
public class ChatbotEstadisticasDTO {

    private long totalConversaciones;
    private long totalMensajes;
    private long totalMensajesUsuario;
    private long totalMensajesBot;
    private long totalErrores;
    private long totalDerivadosSoporte;
    private long usuariosUnicos;
    private double latenciaPromedioMs;
    private long feedbackPositivo;
    private long feedbackNegativo;

    /** Mensajes por dia para el chart de barras apiladas (USER vs BOT) */
    private List<Map<String, Object>> mensajesPorDia;
    /** Distribucion por rol de usuario (para chart de dona) */
    private List<Map<String, Object>> conversacionesPorRol;
    /** Top 5 intents detectados */
    private List<Map<String, Object>> topIntents;
    /** Top 5 usuarios con mas conversaciones */
    private List<Map<String, Object>> topUsuarios;

    public long getTotalConversaciones() { return totalConversaciones; }
    public void setTotalConversaciones(long totalConversaciones) { this.totalConversaciones = totalConversaciones; }
    public long getTotalMensajes() { return totalMensajes; }
    public void setTotalMensajes(long totalMensajes) { this.totalMensajes = totalMensajes; }
    public long getTotalMensajesUsuario() { return totalMensajesUsuario; }
    public void setTotalMensajesUsuario(long totalMensajesUsuario) { this.totalMensajesUsuario = totalMensajesUsuario; }
    public long getTotalMensajesBot() { return totalMensajesBot; }
    public void setTotalMensajesBot(long totalMensajesBot) { this.totalMensajesBot = totalMensajesBot; }
    public long getTotalErrores() { return totalErrores; }
    public void setTotalErrores(long totalErrores) { this.totalErrores = totalErrores; }
    public long getTotalDerivadosSoporte() { return totalDerivadosSoporte; }
    public void setTotalDerivadosSoporte(long totalDerivadosSoporte) { this.totalDerivadosSoporte = totalDerivadosSoporte; }
    public long getUsuariosUnicos() { return usuariosUnicos; }
    public void setUsuariosUnicos(long usuariosUnicos) { this.usuariosUnicos = usuariosUnicos; }
    public double getLatenciaPromedioMs() { return latenciaPromedioMs; }
    public void setLatenciaPromedioMs(double latenciaPromedioMs) { this.latenciaPromedioMs = latenciaPromedioMs; }
    public long getFeedbackPositivo() { return feedbackPositivo; }
    public void setFeedbackPositivo(long feedbackPositivo) { this.feedbackPositivo = feedbackPositivo; }
    public long getFeedbackNegativo() { return feedbackNegativo; }
    public void setFeedbackNegativo(long feedbackNegativo) { this.feedbackNegativo = feedbackNegativo; }
    public List<Map<String, Object>> getMensajesPorDia() { return mensajesPorDia; }
    public void setMensajesPorDia(List<Map<String, Object>> mensajesPorDia) { this.mensajesPorDia = mensajesPorDia; }
    public List<Map<String, Object>> getConversacionesPorRol() { return conversacionesPorRol; }
    public void setConversacionesPorRol(List<Map<String, Object>> conversacionesPorRol) { this.conversacionesPorRol = conversacionesPorRol; }
    public List<Map<String, Object>> getTopIntents() { return topIntents; }
    public void setTopIntents(List<Map<String, Object>> topIntents) { this.topIntents = topIntents; }
    public List<Map<String, Object>> getTopUsuarios() { return topUsuarios; }
    public void setTopUsuarios(List<Map<String, Object>> topUsuarios) { this.topUsuarios = topUsuarios; }
}
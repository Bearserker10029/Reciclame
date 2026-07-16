package org.reciclame.demo.dto;

import java.time.Instant;
import java.util.List;

/**
 * Fila de la tabla "Listado de conversaciones" que se muestra en
 * la UI de Chatbot Analytics (Admin y SuperAdmin).
 */
public class ChatbotConsultaFilaDTO {

    private Long conversacionId;
    private String titulo;
    private Instant fechaInicio;
    private Instant fechaUltimoMensaje;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioCorreo;
    private String usuarioRol;
    private String usuarioEmpresa;
    private Integer totalMensajes;
    private Long duracionTotalMs;
    private String primeraPregunta;
    private String ultimaRespuesta;
    private Integer errores;
    private Boolean derivadoSoporte;
    private String idioma;
    private String ipOrigen;
    private List<MensajeDetalleDTO> mensajes;

    public static class MensajeDetalleDTO {
        private Long id;
        private String rol;
        private String contenido;
        private String intentDetectado;
        private Long duracionMs;
        private Integer tokensEstimados;
        private String modelo;
        private String mensajeError;
        private String sqlEjecutado;
        private Integer filasDevueltas;
        private String metadata;
        private String ipAddress;
        private Boolean esError;
        private Instant createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
        public String getContenido() { return contenido; }
        public void setContenido(String contenido) { this.contenido = contenido; }
        public String getIntentDetectado() { return intentDetectado; }
        public void setIntentDetectado(String intentDetectado) { this.intentDetectado = intentDetectado; }
        public Long getDuracionMs() { return duracionMs; }
        public void setDuracionMs(Long duracionMs) { this.duracionMs = duracionMs; }
        public Integer getTokensEstimados() { return tokensEstimados; }
        public void setTokensEstimados(Integer tokensEstimados) { this.tokensEstimados = tokensEstimados; }
        public String getModelo() { return modelo; }
        public void setModelo(String modelo) { this.modelo = modelo; }
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
        public Boolean getEsError() { return esError; }
        public void setEsError(Boolean esError) { this.esError = esError; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public Long getConversacionId() { return conversacionId; }
    public void setConversacionId(Long conversacionId) { this.conversacionId = conversacionId; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }
    public Instant getFechaUltimoMensaje() { return fechaUltimoMensaje; }
    public void setFechaUltimoMensaje(Instant fechaUltimoMensaje) { this.fechaUltimoMensaje = fechaUltimoMensaje; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public String getUsuarioCorreo() { return usuarioCorreo; }
    public void setUsuarioCorreo(String usuarioCorreo) { this.usuarioCorreo = usuarioCorreo; }
    public String getUsuarioRol() { return usuarioRol; }
    public void setUsuarioRol(String usuarioRol) { this.usuarioRol = usuarioRol; }
    public String getUsuarioEmpresa() { return usuarioEmpresa; }
    public void setUsuarioEmpresa(String usuarioEmpresa) { this.usuarioEmpresa = usuarioEmpresa; }
    public Integer getTotalMensajes() { return totalMensajes; }
    public void setTotalMensajes(Integer totalMensajes) { this.totalMensajes = totalMensajes; }
    public Long getDuracionTotalMs() { return duracionTotalMs; }
    public void setDuracionTotalMs(Long duracionTotalMs) { this.duracionTotalMs = duracionTotalMs; }
    public String getPrimeraPregunta() { return primeraPregunta; }
    public void setPrimeraPregunta(String primeraPregunta) { this.primeraPregunta = primeraPregunta; }
    public String getUltimaRespuesta() { return ultimaRespuesta; }
    public void setUltimaRespuesta(String ultimaRespuesta) { this.ultimaRespuesta = ultimaRespuesta; }
    public Integer getErrores() { return errores; }
    public void setErrores(Integer errores) { this.errores = errores; }
    public Boolean getDerivadoSoporte() { return derivadoSoporte; }
    public void setDerivadoSoporte(Boolean derivadoSoporte) { this.derivadoSoporte = derivadoSoporte; }
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public List<MensajeDetalleDTO> getMensajes() { return mensajes; }
    public void setMensajes(List<MensajeDetalleDTO> mensajes) { this.mensajes = mensajes; }
}
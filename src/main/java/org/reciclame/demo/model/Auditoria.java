package org.reciclame.demo.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 255)
    private String usuarioNombre;

    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    @Column(name = "modulo", nullable = false, length = 100)
    private String modulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "datos_anteriores", columnDefinition = "JSON")
    private String datosAnteriores;

    @Column(name = "datos_nuevos", columnDefinition = "JSON")
    private String datosNuevos;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fecha_accion", nullable = false)
    private Instant fechaAccion;

    @Column(name = "estado", length = 20)
    private String estado; // EXITOSO, FALLIDO, ERROR

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "estado_activo", nullable = false)
    private Boolean estadoActivo = true;

    // ── Cadena de integridad (tamper-evident hash chain) ───────────────
    /**
     * Hash SHA-256 en hex (64 chars) del registro anterior en la cadena.
     * Para el primer registro (génesis) será siempre
     * {@code SHA-256(GENESIS_SALT)} — ver {@code AuditoriaHashChainService}.
     * Si es {@code null} indica que la fila fue insertada antes del
     * despliegue del hash chain y debe re-firmarse con la migración SQL.
     */
    @Column(name = "hash_anterior", length = 64)
    private String hashAnterior;

    /**
     * Hash SHA-256 en hex (64 chars) calculado sobre
     * {@code (hash_anterior || id || fecha_accion_iso || accion || modulo ||
     * usuario_id || descripcion || estado)}. Cualquier edición posterior en
     * cualquiera de esos campos rompe la verificación.
     */
    @Column(name = "hash_actual", length = 64)
    private String hashActual;

    public Auditoria() {
        this.fechaAccion = Instant.now();
        this.estado = "EXITOSO";
    }

    public Auditoria(Long usuarioId, String usuarioNombre, String accion, String modulo, String descripcion) {
        this();
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.accion = accion;
        this.modulo = modulo;
        this.descripcion = descripcion;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getModulo() {
        return modulo;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDatosAnteriores() {
        return datosAnteriores;
    }

    public void setDatosAnteriores(String datosAnteriores) {
        this.datosAnteriores = datosAnteriores;
    }

    public String getDatosNuevos() {
        return datosNuevos;
    }

    public void setDatosNuevos(String datosNuevos) {
        this.datosNuevos = datosNuevos;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getFechaAccion() {
        return fechaAccion;
    }

    public void setFechaAccion(Instant fechaAccion) {
        this.fechaAccion = fechaAccion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public Boolean getEstadoActivo() {
        return estadoActivo;
    }

    public void setEstadoActivo(Boolean estadoActivo) {
        this.estadoActivo = estadoActivo;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }

    public String getHashActual() {
        return hashActual;
    }

    public void setHashActual(String hashActual) {
        this.hashActual = hashActual;
    }
}

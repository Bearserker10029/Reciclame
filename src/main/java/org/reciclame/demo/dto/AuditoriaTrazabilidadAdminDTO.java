package org.reciclame.demo.dto;

import org.reciclame.demo.model.Auditoria;

import java.time.Instant;

/**
 * DTO de trazabilidad que se entrega a la UI del rol ADMIN.
 *
 * <h2>Por qué existe</h2>
 * Antes, la página {@code /admin/trazabilidad} recibía la entidad
 * {@link Auditoria} directamente, lo que exponía en el modelo Thymeleaf
 * datos sensibles de auditoría de seguridad:
 *
 * <ul>
 *   <li>{@code ipAddress} (IP pública del cliente) — RGPD / ISO 27001</li>
 *   <li>{@code userAgent}</li>
 *   <li>{@code datosAnteriores} / {@code datosNuevos} (JSON con datos personales)</li>
 *   <li>{@code mensajeError} (detalles internos)</li>
 *   <li>{@code hashAnterior} / {@code hashActual} (metadata de integridad)</li>
 * </ul>
 *
 * <p>El ADMIN no necesita ninguno de esos para su labor de gestión; esa
 * información es exclusiva del SUPERADMIN (trazabilidad de seguridad).
 * Este DTO aplica el principio de <b>mínimo privilegio</b> en la capa
 * de presentación: solo expone los campos que el admin realmente usa.</p>
 */
public class AuditoriaTrazabilidadAdminDTO {

    private final Long id;
    private final Instant fechaAccion;
    private final Long usuarioId;
    private final String usuarioNombre;
    private final String accion;
    private final String modulo;
    private final String descripcion;
    private final String estado;

    public AuditoriaTrazabilidadAdminDTO(Long id, Instant fechaAccion, Long usuarioId,
                                          String usuarioNombre, String accion, String modulo,
                                          String descripcion, String estado) {
        this.id = id;
        this.fechaAccion = fechaAccion;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.accion = accion;
        this.modulo = modulo;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public static AuditoriaTrazabilidadAdminDTO from(Auditoria a) {
        if (a == null) return null;
        return new AuditoriaTrazabilidadAdminDTO(
                a.getId(),
                a.getFechaAccion(),
                a.getUsuarioId(),
                a.getUsuarioNombre(),
                a.getAccion(),
                a.getModulo(),
                a.getDescripcion(),
                a.getEstado()
        );
    }

    public Long getId() { return id; }
    public Instant getFechaAccion() { return fechaAccion; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public String getAccion() { return accion; }
    public String getModulo() { return modulo; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
}
package org.reciclame.demo.service;

import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.repository.AuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    // ── CREAR AUDITORÍA ──────────────────────────────────────────────
    @Transactional
    public Auditoria registrarAccion(Long usuarioId, String usuarioNombre, String accion,
                                    String modulo, String descripcion) {
        return registrarAccion(usuarioId, usuarioNombre, accion, modulo, descripcion,
                             null, null, "EXITOSO", null, null, null);
    }

    @Transactional
    public Auditoria registrarAccion(Long usuarioId, String usuarioNombre, String accion,
                                    String modulo, String descripcion, String datosAnteriores,
                                    String datosNuevos) {
        return registrarAccion(usuarioId, usuarioNombre, accion, modulo, descripcion,
                             datosAnteriores, datosNuevos, "EXITOSO", null, null, null);
    }

    @Transactional
    public Auditoria registrarAccionFallida(Long usuarioId, String usuarioNombre, String accion,
                                           String modulo, String descripcion, String mensajeError,
                                           String ipAddress, String userAgent) {
        return registrarAccion(usuarioId, usuarioNombre, accion, modulo, descripcion,
                             null, null, "FALLIDO", mensajeError, ipAddress, userAgent);
    }

    @Transactional
    public Auditoria registrarAccion(Long usuarioId, String usuarioNombre, String accion,
                                    String modulo, String descripcion, String datosAnteriores,
                                    String datosNuevos, String estado, String mensajeError,
                                    String ipAddress, String userAgent) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setUsuarioId(usuarioId);
            auditoria.setUsuarioNombre(usuarioNombre != null ? usuarioNombre : "Sistema");
            auditoria.setAccion(accion);
            auditoria.setModulo(modulo);
            auditoria.setDescripcion(descripcion);
            auditoria.setDatosAnteriores(datosAnteriores);
            auditoria.setDatosNuevos(datosNuevos);
            auditoria.setEstado(estado != null ? estado : "EXITOSO");
            auditoria.setMensajeError(mensajeError);
            auditoria.setIpAddress(ipAddress);
            auditoria.setUserAgent(userAgent);
            auditoria.setFechaAccion(Instant.now());

            return auditoriaRepository.save(auditoria);
        } catch (Exception e) {
            System.err.println("Error al registrar acción de auditoría: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ── READ ─────────────────────────────────────────────────────────
    public List<Auditoria> listarAuditorias() {
        return auditoriaRepository.findAll();
    }

    public Page<Auditoria> listarAuditoriasPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findAllPaginado(pageable);
    }

    public List<Auditoria> listarAuditoriasPorUsuario(Long usuarioId) {
        return auditoriaRepository.findByUsuarioId(usuarioId);
    }

    public Page<Auditoria> listarAuditoriasPorUsuarioPaginado(Long usuarioId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByUsuarioIdPaginado(usuarioId, pageable);
    }

    public List<Auditoria> listarAuditoriasPorModulo(String modulo) {
        return auditoriaRepository.findByModulo(modulo);
    }

    public Page<Auditoria> listarAuditoriasPorModuloPaginado(String modulo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByModuloPaginado(modulo, pageable);
    }

    public List<Auditoria> listarAuditoriasPorAccion(String accion) {
        return auditoriaRepository.findByAccion(accion);
    }

    public Page<Auditoria> listarAuditoriasPorAccionPaginado(String accion, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByAccionPaginado(accion, pageable);
    }

    public List<Auditoria> listarAuditoriasPorFechaRango(Instant desde, Instant hasta) {
        return auditoriaRepository.findByFechaRango(desde, hasta);
    }

    public Auditoria obtenerAuditoriaPorId(Long id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auditoría no encontrada con id: " + id));
    }

    // ── CONSTANTES ───────────────────────────────────────────────────
    public static final String ACCION_CREAR_USUARIO = "CREAR_USUARIO";
    public static final String ACCION_EDITAR_USUARIO = "EDITAR_USUARIO";
    public static final String ACCION_ELIMINAR_USUARIO = "ELIMINAR_USUARIO";
    public static final String ACCION_BLOQUEAR_USUARIO = "BLOQUEAR_USUARIO";
    public static final String ACCION_DESBLOQUEAR_USUARIO = "DESBLOQUEAR_USUARIO";
    public static final String ACCION_APROBAR_USUARIO = "APROBAR_USUARIO";
    public static final String ACCION_RECHAZAR_USUARIO = "RECHAZAR_USUARIO";
    public static final String ACCION_LOGIN = "LOGIN";
    public static final String ACCION_LOGOUT = "LOGOUT";
    public static final String ACCION_CAMBIAR_PASSWORD = "CAMBIAR_PASSWORD";
    public static final String ACCION_IMPERSONAR = "IMPERSONAR_USUARIO";
    public static final String ACCION_SALIR_IMPERSONACION = "SALIR_IMPERSONACION";
    public static final String ACCION_REACTIVAR_USUARIO = "REACTIVAR_USUARIO";

    public static final String MODULO_USUARIOS = "USUARIOS";
    public static final String MODULO_NORMATIVAS = "NORMATIVAS";
    public static final String MODULO_ESTUDIOS = "ESTUDIOS";
    public static final String MODULO_EMPRESAS = "EMPRESAS";
    public static final String MODULO_SISTEMA = "SISTEMA";
    public static final String MODULO_SEGURIDAD = "SEGURIDAD";
}

package org.reciclame.demo.service;

import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.repository.AuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        var result = auditoriaRepository.findAll();
        if (result == null) return java.util.Collections.emptyList();
        return result.stream()
                .filter(a -> a.getAccion() == null ||
                        (!"IMPERSONAR_USUARIO".equals(a.getAccion()) &&
                         !"SALIR_IMPERSONACION".equals(a.getAccion())))
                .collect(Collectors.toList());
    }

    public Page<Auditoria> listarAuditoriasPaginado(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findAllPaginado(pageable);
    }

    public Page<Auditoria> listarAuditoriasParaTrazabilidad(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findAllExcludingSeguridad(pageable);
    }

    public Page<Auditoria> listarAuditoriasParaTrazabilidadAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findAllExcludingSeguridadYAccionesDeSuperadmin(pageable);
    }

    public List<Auditoria> listarAuditoriasLimit(int limite) {
        var stream = auditoriaRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getFechaAccion() == null) return 1;
                    if (b.getFechaAccion() == null) return -1;
                    return b.getFechaAccion().compareTo(a.getFechaAccion());
                });
        if (limite > 0) stream = stream.limit(limite);
        return stream.collect(Collectors.toList());
    }

    /**
     * Página de auditorías excluyendo acciones de usuarios con rol SUPERADMIN.
     * Usado por la UI del ADMIN para que no vea nada del superadmin.
     */
    public Page<Auditoria> findAllExcluyendoAccionesDeSuperadmin(int page, int size) {
        return auditoriaRepository.findAllExcluyendoAccionesDeSuperadmin(
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    /**
     * Lista auditorías recientes con un límite, excluyendo las acciones
     * ejecutadas por usuarios con rol SUPERADMIN (subquery contra Usuario/Rol
     * en JPQL — no depende de carga LAZY).
     *
     <p>Si {@code limite <= 0} se devuelven todas (paginado completo, capped
     * a 5000 filas para no reventar memoria).</p>
     */
    public List<Auditoria> listarAuditoriasLimitExcluyendoSuperadmin(int limite) {
        int size = limite > 0 ? Math.min(limite, 5000) : 5000;
        var page = auditoriaRepository.findAllExcluyendoAccionesDeSuperadminP(
                org.springframework.data.domain.PageRequest.of(0, size));
        return page.getContent();
    }

    /**
     * Variante "últimas 100" del filtro anterior. Se usa en los exports
     * legacy y en el CSV.
     */
    public List<Auditoria> listarAuditoriasUltimas100ExcluyendoSuperadmin() {
        return auditoriaRepository.findAllExcluyendoAccionesDeSuperadminP(
                org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
    }

    public List<Auditoria> listarAuditoriasUltimas100() {
        return auditoriaRepository.findTop100ByOrderByFechaAccionDesc();
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

    public Page<Auditoria> listarAuditoriasPorUsuarioId(int page, int size, Long usuarioId) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByUsuarioIdExcludingImpersonacion(usuarioId, pageable);
    }

    public List<Auditoria> listarAuditoriasPorAccion(String accion) {
        return auditoriaRepository.findByAccion(accion);
    }

    public Page<Auditoria> listarAuditoriasPorAccionPaginado(String accion, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findByAccionPaginado(accion, pageable);
    }

    public List<Map<String, Object>> agregarPorModulo() {
        List<Auditoria> auditorias = listarAuditorias();
        return auditorias.stream()
                .filter(a -> a.getModulo() != null)
                .collect(Collectors.groupingBy(Auditoria::getModulo, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> agregarPorEstado() {
        List<Auditoria> auditorias = listarAuditorias();
        return auditorias.stream()
                .filter(a -> a.getEstado() != null)
                .collect(Collectors.groupingBy(Auditoria::getEstado, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Auditoria> listarAuditoriasPorFechaRango(Instant desde, Instant hasta) {
        var result = auditoriaRepository.findByFechaRango(desde, hasta);
        return result != null ? result : java.util.Collections.emptyList();
    }

    public List<Map<String, Object>> obtenerActividadDiaria(Instant desde, Instant hasta) {
        var auditorias = listarAuditoriasPorFechaRango(desde, hasta);
        if (auditorias == null || auditorias.isEmpty()) return java.util.Collections.emptyList();
        return auditorias.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getFechaAccion().toString().substring(0, 10),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("fecha", e.getKey());
                    item.put("count", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerTopUsuariosActivos(int limit) {
        var auditorias = listarAuditorias();
        if (auditorias == null || auditorias.isEmpty()) return java.util.Collections.emptyList();
        Map<Long, String> nombrePorId = new HashMap<>();
        Map<Long, Long> conteoPorId = new HashMap<>();

        for (Auditoria a : auditorias) {
            if (a.getUsuarioId() != null && a.getUsuarioNombre() != null) {
                nombrePorId.put(a.getUsuarioId(), a.getUsuarioNombre());
                conteoPorId.put(a.getUsuarioId(), conteoPorId.getOrDefault(a.getUsuarioId(), 0L) + 1);
            }
        }

        return conteoPorId.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("usuarioId", e.getKey());
                    item.put("usuarioNombre", nombrePorId.get(e.getKey()));
                    item.put("interacciones", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public Auditoria obtenerAuditoriaPorId(Long id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auditoría no encontrada con id: " + id));
    }

    // ── AUDITORÍA DE SEGURIDAD ───────────────────────────────────────
    public Page<Auditoria> listarEventosSeguridad(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditoriaRepository.findEventosSeguridad(pageable);
    }

    public List<Map<String, Object>> obtenerTopIPsFallidas(int limit) {
        var result = auditoriaRepository.topIPsFallidas(PageRequest.of(0, limit));
        if (result == null) return java.util.Collections.emptyList();
        return result.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("ip", row[0]);
                    item.put("count", ((Number) row[1]).intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> obtenerTopUsuariosFallidos(int limit) {
        var result = auditoriaRepository.topUsuariosFallidos(PageRequest.of(0, limit));
        if (result == null) return java.util.Collections.emptyList();
        return result.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("usuarioId", row[0]);
                    item.put("usuarioNombre", row[1]);
                    item.put("count", ((Number) row[2]).intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public long contarIntentosFallidosHoy() {
        Instant hace24h = Instant.now().minusSeconds(86400);
        return auditoriaRepository.contarFallidosDesde(hace24h);
    }

    public long contarImpersonaciones() {
        return auditoriaRepository.contarImpersonaciones();
    }

    public List<Map<String, Object>> obtenerConteoPorModulo() {
        var result = auditoriaRepository.conteoPorModulo();
        if (result == null) return java.util.Collections.emptyList();
        return result.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("modulo", row[0]);
                    item.put("count", ((Number) row[1]).intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<String> obtenerModulosDistinct() {
        var result = auditoriaRepository.modulosDistinct();
        return result != null ? result : java.util.Collections.emptyList();
    }

    public List<String> generarAlertasSeguridad() {
        List<String> alertas = new java.util.ArrayList<>();
        Instant hace24h = Instant.now().minusSeconds(86400);
        var todas = listarAuditoriasPorFechaRango(hace24h, Instant.now());
        if (todas == null) return alertas;
        List<Auditoria> fallidos24h = todas.stream()
                .filter(a -> "FALLIDO".equals(a.getEstado()))
                .collect(Collectors.toList());

        Map<String, Long> fallosPorIP = fallidos24h.stream()
                .filter(a -> a.getIpAddress() != null)
                .collect(Collectors.groupingBy(Auditoria::getIpAddress, Collectors.counting()));

        fallosPorIP.forEach((ip, count) -> {
            if (count >= 3) {
                alertas.add("⚠ " + count + " intentos fallidos desde IP " + ip + " en las últimas 24h");
            }
        });

        Map<String, Long> fallosPorUsuario = fallidos24h.stream()
                .filter(a -> a.getUsuarioNombre() != null)
                .collect(Collectors.groupingBy(Auditoria::getUsuarioNombre, Collectors.counting()));

        fallosPorUsuario.forEach((usuario, count) -> {
            if (count >= 3) {
                alertas.add("⚠ Usuario " + usuario + " tiene " + count + " acciones fallidas en 24h");
            }
        });

        long impersonaciones = contarImpersonaciones();
        if (impersonaciones > 0) {
            alertas.add("ℹ " + impersonaciones + " sesión(es) de impersonación registrada(s)");
        }

        return alertas;
    }

    public double obtenerTasaExito24h() {
        Instant hace24h = Instant.now().minusSeconds(86400);
        var auditorias24h = listarAuditoriasPorFechaRango(hace24h, Instant.now());
        if (auditorias24h == null || auditorias24h.isEmpty()) return 100.0;
        long exitosos = auditorias24h.stream()
                .filter(a -> "EXITOSO".equals(a.getEstado()))
                .count();
        return Math.round((exitosos * 100.0 / auditorias24h.size()) * 10.0) / 10.0;
    }

    public long contarEntreFechas(Instant desde, Instant hasta) {
        return auditoriaRepository.contarEntreFechas(desde, hasta);
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
    public static final String ACCION_DESCARGAR_DOCUMENTO = "DESCARGAR_DOCUMENTO";

    // ── NORMATIVAS ───────────────────────────────────────────────
    public static final String ACCION_CREAR_NORMATIVA = "CREAR_NORMATIVA";
    public static final String ACCION_EDITAR_NORMATIVA = "EDITAR_NORMATIVA";
    public static final String ACCION_ELIMINAR_NORMATIVA = "ELIMINAR_NORMATIVA";

    // ── ESTUDIOS ─────────────────────────────────────────────────
    public static final String ACCION_CREAR_ESTUDIO = "CREAR_ESTUDIO";
    public static final String ACCION_EDITAR_ESTUDIO = "EDITAR_ESTUDIO";
    public static final String ACCION_ELIMINAR_ESTUDIO = "ELIMINAR_ESTUDIO";

    public static final String MODULO_USUARIOS = "USUARIOS";
    public static final String MODULO_NORMATIVAS = "NORMATIVAS";
    public static final String MODULO_ESTUDIOS = "ESTUDIOS";
    public static final String MODULO_EMPRESAS = "EMPRESAS";
    public static final String MODULO_SISTEMA = "SISTEMA";
    public static final String MODULO_SEGURIDAD = "SEGURIDAD";
}

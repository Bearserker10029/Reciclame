package org.reciclame.demo.service;

import org.reciclame.demo.dto.UsuarioRequestDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.model.Empresa;
import org.reciclame.demo.model.Rol;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.EmpresaRepository;
import org.reciclame.demo.repository.RolRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;
    private final AuditoriaService auditoriaService;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;
    private final EmailNotificationService emailService;
    private final ObjectMapper objectMapper;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                         EmpresaRepository empresaRepository, AuditoriaService auditoriaService,
                         PasswordEncoder passwordEncoder, NotificacionService notificacionService,
                         EmailNotificationService emailService,
                         FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.sessionRepository = sessionRepository;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // java.time.Instant (JSR-310), etc.
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // ── Guarda de privilegios: solo SUPERADMIN puede otorgar el rol
    // SUPERADMIN o tocar una cuenta que ya es SUPERADMIN. Sin esto, el
    // combo de rol en /admin/usuarios incluía SUPERADMIN como opción
    // seleccionable y cualquier ADMINISTRADOR podía auto-asignárselo (o
    // asignárselo a otro), o editar/degradar a un SuperAdmin existente
    // adivinando su id. auditorUsuarioId == null significa llamada
    // interna del sistema (los overloads "sin auditoría"), no HTTP: se
    // deja pasar.
    private void verificarPermisoAsignacionRol(Long auditorUsuarioId, Rol rolDestino, Usuario usuarioObjetivo) {
        if (auditorUsuarioId == null) {
            return;
        }
        boolean esSuperadmin = usuarioRepository.findById(auditorUsuarioId)
                .map(u -> u.getRol() != null && "SUPERADMIN".equals(u.getRol().getNombre()))
                .orElse(false);
        if (esSuperadmin) {
            return;
        }
        if (rolDestino != null && "SUPERADMIN".equals(rolDestino.getNombre())) {
            throw new RuntimeException("No tienes permisos para asignar el rol SUPERADMIN.");
        }
        if (usuarioObjetivo != null && usuarioObjetivo.getRol() != null
                && "SUPERADMIN".equals(usuarioObjetivo.getRol().getNombre())) {
            throw new RuntimeException("No tienes permisos para modificar una cuenta SUPERADMIN.");
        }
    }

    // ── Guarda anti auto-acción ──────────────────────────────
    // Evita que un administrador se desactive, se bloquee, se elimine
    // o se cambie el rol a sí mismo desde el panel admin. El mismo
    // SUPERADMIN queda cubierto: si bien puede modificar a otros
    // SUPERADMIN, no debe sacarse a sí mismo del sistema.
    // aud == null indica llamada interna del sistema (no HTTP), se
    // permite. La edición de datos básicos (nombre, cargo) NO entra
    // acá: el usuario puede actualizar su propio nombre/cargo desde
    // la vista de perfil.
    private void verificarNoAutoAccion(Long auditorUsuarioId, Long objetivoUsuarioId, String accion) {
        if (auditorUsuarioId == null || objetivoUsuarioId == null) {
            return;
        }
        if (auditorUsuarioId.equals(objetivoUsuarioId)) {
            throw new RuntimeException("No puedes " + accion + " tu propia cuenta desde el panel de administración. Usa la vista de Perfil para cambios sobre tu usuario.");
        }
    }

    // ── Invalidación de sesión activa ───────────────────────────
    // Los authorities de Spring Security se fijan en el login y quedan
    // cacheados en la sesión (HttpSession vía Spring Session JDBC/Redis según
    // el perfil). Si un SuperAdmin cambia el rol de un usuario, lo bloquea o
    // lo desactiva mientras ese usuario tiene una sesión abierta, sin esto
    // seguía operando con sus privilegios viejos hasta que la sesión
    // expirara sola o volviera a loguearse. Se apoya en el índice por
    // "principal name" que Spring Session expone out-of-the-box (el
    // principal es el correo corporativo, ver GestionUsuarioService).
    private void invalidarSesionesDe(String correoCorporativo) {
        if (correoCorporativo == null) {
            return;
        }
        try {
            sessionRepository.findByPrincipalName(correoCorporativo).keySet()
                    .forEach(sessionRepository::deleteById);
        } catch (Exception ex) {
            // No bloquear la operación administrativa si falla la invalidación.
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        if (usuarioRepository.existsByCorreoCorporativo(dto.getCorreoCorporativo())) {
            auditoriaService.registrarAccionFallida(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Intento crear usuario con correo duplicado: " + dto.getCorreoCorporativo(),
                "El correo ya existe en el sistema", ipAddress, userAgent);
            throw new RuntimeException("Ya existe un usuario con ese correo corporativo.");
        }
        if (usuarioRepository.existsByDni(dto.getDni())) {
            auditoriaService.registrarAccionFallida(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Intento crear usuario con DNI duplicado: " + dto.getDni(),
                "El DNI ya existe en el sistema", ipAddress, userAgent);
            throw new RuntimeException("Ya existe un usuario con ese DNI.");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + dto.getRolId()));
        verificarPermisoAsignacionRol(auditorUsuarioId, rol, null);

        Empresa empresa = null;
        if (dto.getEmpresaId() != null) {
            empresa = empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + dto.getEmpresaId()));
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setDni(dto.getDni());
        usuario.setCorreoCorporativo(dto.getCorreoCorporativo());
        usuario.setPasswordHash(hashPassword(dto.getPassword()));
        // La define el admin como contraseña inicial; se exige cambiarla en el
        // primer login (ver ForzarCambioPasswordFilter), igual que el flujo de
        // aprobación de solicitudes.
        usuario.setDebeCambiarPassword(true);
        usuario.setCargo(dto.getCargo());
        usuario.setFotoUrl(dto.getFotoUrl());
        usuario.setRol(rol);
        usuario.setEmpresa(empresa);
        usuario.setEstadoCuenta("ACTIVO");
        usuario.setEstadoActivo(true);
        usuario.setCuentaBloqueada(false);
        usuario.setTfaActivo(false);
        usuario.setIntentosFallidos((byte) 0);
        usuario.setCreatedAt(Instant.now());
        usuario.setUpdatedAt(Instant.now());

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        try {
            String datosNuevos = objectMapper.writeValueAsString(toResponseDTO(usuarioGuardado));
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Crear usuario: " + usuario.getNombreCompleto() + " (" + usuario.getCorreoCorporativo() + ")",
                null, datosNuevos, "EXITOSO", null, ipAddress, userAgent);
        } catch (Exception e) {
            System.err.println("Error al serializar datos de auditoría: " + e.getMessage());
            e.printStackTrace();
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
                "Crear usuario: " + usuario.getNombreCompleto() + " (" + usuario.getCorreoCorporativo() + ")",
                null, null, "EXITOSO", null, ipAddress, userAgent);
        }

        return toResponseDTO(usuarioGuardado);
    }

    // ── CREATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto) {
        return crearUsuario(dto, null, "Sistema", null, null);
    }

    // ── FIND BY CORREO ────────────────────────────────────────
    public java.util.Optional<UsuarioResponseDTO> findByCorreo(String correo) {
        return usuarioRepository.findByCorreoCorporativo(correo).map(this::toResponseDTO);
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosUltimos100() {
        return usuarioRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosLimit(int limite) {
        var stream = usuarioRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
        if (limite > 0) stream = stream.limit(limite);
        return stream.map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosActivos() {
        return usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstadoActivo()))
                .filter(u -> u.getRol() != null && !"SUPERADMIN".equals(u.getRol().getNombre()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosNoSuperadmin() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == null || !"SUPERADMIN".equals(u.getRol().getNombre()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el conjunto de IDs de usuarios con rol SUPERADMIN.
     * Útil para filtrar listados donde la entidad referenciada solo guarda usuarioId
     * (p. ej. Auditoria) y se requiere ocultar las acciones de superadmin en el
     * contexto del rol ADMIN.
     */
    public java.util.Set<Long> idsUsuariosSuperadmin() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && "SUPERADMIN".equals(u.getRol().getNombre()))
                .map(org.reciclame.demo.model.Usuario::getId)
                .collect(Collectors.toCollection(java.util.HashSet::new));
    }

    public List<UsuarioResponseDTO> listarUsuariosPendientes() {
        return usuarioRepository.findAll().stream()
                .filter(u -> "PENDIENTE".equals(u.getEstadoCuenta()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public long contarNuevosDesde(Instant desde) {
        return usuarioRepository.contarNuevosDesde(desde);
    }

    public long contarEntreFechas(Instant desde, Instant hasta) {
        return usuarioRepository.contarEntreFechas(desde, hasta);
    }

    public List<Map<String, Object>> agregarPorEstado() {
        List<UsuarioResponseDTO> usuarios = listarUsuarios();
        return usuarios.stream()
                .collect(Collectors.groupingBy(u -> u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO", Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> agregarPorRol() {
        List<UsuarioResponseDTO> usuarios = listarUsuarios();
        return usuarios.stream()
                .filter(u -> u.getRolNombre() != null)
                .collect(Collectors.groupingBy(UsuarioResponseDTO::getRolNombre, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public UsuarioResponseDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return toResponseDTO(usuario);
    }

    public Usuario obtenerUsuarioEntityPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioRequestDTO dto, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        verificarPermisoAsignacionRol(auditorUsuarioId, null, usuario);

        String datosAnteriores = null;
        String datosNuevos = null;

        try {
            datosAnteriores = objectMapper.writeValueAsString(toResponseDTO(usuario));
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo serializar datos anteriores: " + e.getMessage());
        }

        if (dto.getNombreCompleto() != null) usuario.setNombreCompleto(dto.getNombreCompleto());
        if (dto.getCorreoCorporativo() != null) usuario.setCorreoCorporativo(dto.getCorreoCorporativo());
        if (dto.getCargo()          != null) usuario.setCargo(dto.getCargo());
        if (dto.getFotoUrl()        != null) usuario.setFotoUrl(dto.getFotoUrl());

        if (dto.getRolId() != null) {
            Rol rol = rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + dto.getRolId()));
            verificarPermisoAsignacionRol(auditorUsuarioId, rol, null);
            boolean cambioDeRol = usuario.getRol() == null || !usuario.getRol().getId().equals(rol.getId());
            if (cambioDeRol) {
                verificarNoAutoAccion(auditorUsuarioId, id, "cambiar el rol de");
            }
            usuario.setRol(rol);
            if (cambioDeRol) {
                invalidarSesionesDe(usuario.getCorreoCorporativo());
            }
        }
        if (dto.getEmpresaId() != null) {
            Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + dto.getEmpresaId()));
            usuario.setEmpresa(empresa);
        }
        // La contraseña de un usuario ya existente NUNCA se toca desde acá:
        // ni admin ni superadmin pueden asignarle/cambiarle la clave a otra
        // persona (solo el propio usuario, vía "olvidé mi contraseña" o el
        // cambio obligatorio de la temporal en su primer login).

        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        try {
            datosNuevos = objectMapper.writeValueAsString(toResponseDTO(usuarioGuardado));
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo serializar datos nuevos: " + e.getMessage());
        }   

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_EDITAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Editar usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            datosAnteriores, datosNuevos, "EXITOSO", null, ipAddress, userAgent);

        return toResponseDTO(usuarioGuardado);
    }

    // ── UPDATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        return actualizarUsuario(id, dto, null, "Sistema", null, null);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    @Transactional
    public void eliminarUsuario(Long id, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        verificarNoAutoAccion(auditorUsuarioId, id, "eliminar");
        usuario.setEstadoActivo(false);
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);
        invalidarSesionesDe(usuario.getCorreoCorporativo());

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_ELIMINAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Eliminar usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);
    }

    // ── DELETE (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public void eliminarUsuario(Long id) {
        eliminarUsuario(id, null, "Sistema", null, null);
    }

    // ── REACTIVAR usuario eliminado ─────────────────────────────
    @Transactional
    public UsuarioResponseDTO reactivarUsuario(Long id, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        verificarNoAutoAccion(auditorUsuarioId, id, "reactivar");

        usuario.setEstadoActivo(true);
        usuario.setEstadoCuenta("ACTIVO");
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_REACTIVAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Reactivar usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

        try { emailService.enviarReactivacion(usuarioGuardado.getCorreoCorporativo(), usuarioGuardado.getNombreCompleto()); } catch (Exception ex) { /* no rollback */ }

        return toResponseDTO(usuarioGuardado);
    }

    // ── REACTIVAR (overload sin auditoría) ─────────────────────────
    @Transactional
    public UsuarioResponseDTO reactivarUsuario(Long id) {
        return reactivarUsuario(id, null, "Sistema", null, null);
    }

    // ── APROBAR solicitud pendiente ───────────────────────────
    @Transactional
    public UsuarioResponseDTO aprobarUsuario(Long id, Long rolId, Long empresaId, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        usuario.setEstadoCuenta("ACTIVO");
        usuario.setEstadoActivo(true);
        if (rolId != null) {
            Rol rol = rolRepository.findById(rolId)
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + rolId));
            verificarPermisoAsignacionRol(auditorUsuarioId, rol, null);
            usuario.setRol(rol);
        }
        if (empresaId != null) {
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + empresaId));
            usuario.setEmpresa(empresa);
        }
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_APROBAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Aprobar usuario pendiente: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

        try { notificacionService.notificarAprobacion(usuarioGuardado.getId()); } catch (Exception ex) { /* no rollback */ }
        try { emailService.enviarAprobacion(usuarioGuardado.getCorreoCorporativo(), usuarioGuardado.getNombreCompleto()); } catch (Exception ex) { /* no rollback */ }

        return toResponseDTO(usuarioGuardado);
    }

    // ── APROBAR (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public UsuarioResponseDTO aprobarUsuario(Long id, Long rolId, Long empresaId) {
        return aprobarUsuario(id, rolId, empresaId, null, "Sistema", null, null);
    }

    // ── RECHAZAR solicitud pendiente ──────────────────────────
    @Transactional
    public UsuarioResponseDTO rechazarUsuario(Long id, String motivo, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setEstadoActivo(false);
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_RECHAZAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Rechazar usuario pendiente: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

        try { notificacionService.notificarRechazo(usuarioGuardado.getId()); } catch (Exception ex) { /* no rollback */ }
        try { emailService.enviarRechazo(usuarioGuardado.getCorreoCorporativo(), usuarioGuardado.getNombreCompleto(), motivo != null ? motivo : "No se proporcionó un motivo"); } catch (Exception ex) { /* no rollback */ }

        return toResponseDTO(usuarioGuardado);
    }

    // ── RECHAZAR (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public UsuarioResponseDTO rechazarUsuario(Long id) {
        return rechazarUsuario(id, null, null, "Sistema", null, null);
    }

    // ── BLOQUEAR / DESBLOQUEAR ────────────────────────────────
    @Transactional
    public UsuarioResponseDTO toggleBloquear(Long id, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        verificarNoAutoAccion(auditorUsuarioId, id, "bloquear/desbloquear");
        boolean bloquear = !Boolean.TRUE.equals(usuario.getCuentaBloqueada());
        if (bloquear) {
            usuario.setCuentaBloqueada(true);
            usuario.setEstadoCuenta("BLOQUEADO");
            // estadoActivo se preserva: si estaba inactivo, sigue inactivo.
        } else {
            usuario.setCuentaBloqueada(false);
            usuario.setEstadoActivo(true);                  // FIX: re-activar al desbloquear
            usuario.setEstadoCuenta("ACTIVO");
        }
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        if (bloquear) {
            invalidarSesionesDe(usuarioGuardado.getCorreoCorporativo());
        }

        String accion = bloquear ? AuditoriaService.ACCION_BLOQUEAR_USUARIO : AuditoriaService.ACCION_DESBLOQUEAR_USUARIO;
        String descripcion = (bloquear ? "Bloquear" : "Desbloquear") + " usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")";
        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            accion, AuditoriaService.MODULO_USUARIOS, descripcion,
            null, null, "EXITOSO", null, ipAddress, userAgent);

        if (bloquear) {
            try { emailService.enviarBloqueo(usuarioGuardado.getCorreoCorporativo(), usuarioGuardado.getNombreCompleto()); } catch (Exception ex) { /* no rollback */ }
        }

        return toResponseDTO(usuarioGuardado);
    }

    // ── BLOQUEAR (overload sin auditoría para compatibilidad) ──────────
    @Transactional
    public UsuarioResponseDTO toggleBloquear(Long id) {
        return toggleBloquear(id, null, "Sistema", null, null);
    }

    // ── EXPORTAR CSV ──────────────────────────────────────────
    @Transactional
    public String exportarCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Nombre Completo,DNI,Correo,Rol,Empresa,Estado,Registro\n");
        usuarioRepository.findAll().forEach(u -> {
            sb.append(u.getId()).append(",");
            sb.append(escapeCsv(u.getNombreCompleto())).append(",");
            sb.append(escapeCsv(u.getDni())).append(",");
            sb.append(escapeCsv(u.getCorreoCorporativo())).append(",");
            sb.append(escapeCsv(u.getRol() != null ? u.getRol().getNombre() : "")).append(",");
            sb.append(escapeCsv(u.getEmpresa() != null ? u.getEmpresa().getRazonSocial() : "")).append(",");
            sb.append(escapeCsv(u.getEstadoCuenta())).append(",");
            sb.append(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "").append("\n");
        });
        return sb.toString();
    }

    // ── HELPERS ───────────────────────────────────────────────
    public List<UsuarioResponseDTO> listarCompanerosEmpresa(Long empresaId, Long excludeId) {
        return usuarioRepository.findCompanerosEmpresa(empresaId, excludeId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    private UsuarioResponseDTO toResponseDTO(Usuario u) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setDni(u.getDni());
        dto.setCorreoCorporativo(u.getCorreoCorporativo());
        dto.setCargo(u.getCargo());
        dto.setFotoUrl(u.getFotoUrl());
        dto.setRolId(u.getRol() != null ? u.getRol().getId() : null);
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : null);
        dto.setEmpresaId(u.getEmpresa() != null ? u.getEmpresa().getId() : null);
        dto.setEmpresaNombre(u.getEmpresa() != null ? u.getEmpresa().getRazonSocial() : null);
        dto.setEstadoCuenta(u.getEstadoCuenta());
        dto.setEstadoActivo(u.getEstadoActivo());
        dto.setCuentaBloqueada(u.getCuentaBloqueada());
        dto.setTfaActivo(u.getTfaActivo());
        dto.setUltimoLogin(u.getUltimoLogin());
        dto.setCreatedAt(u.getCreatedAt());
        dto.setUpdatedAt(u.getUpdatedAt());
        return dto;
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Transactional
    public boolean cambiarPassword(Long usuarioId, String passwordActual, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + usuarioId));

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            return false;
        }

        // Defensa del lado servidor: la nueva contraseña NO puede ser igual
        // a la actual. El frontend (password-validator) ya bloquea este caso
        // en cliente, pero esto protege contra JS deshabilitado, peticiones
        // forjadas o cualquier ruta que se salte la validación visual.
        if (passwordEncoder.matches(nuevaPassword, usuario.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "La nueva contraseña no puede ser igual a la contraseña actual");
        }

        usuario.setPasswordHash(hashPassword(nuevaPassword));
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);

        auditoriaService.registrarAccion(usuarioId, usuario.getNombreCompleto(),
                AuditoriaService.ACCION_CAMBIAR_PASSWORD, AuditoriaService.MODULO_SEGURIDAD,
                "Cambio de contraseña exitoso",
                null, null, "EXITOSO", null, null, null);

        return true;
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }
}

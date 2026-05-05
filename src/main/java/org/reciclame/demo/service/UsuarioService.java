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
    private final ObjectMapper objectMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                         EmpresaRepository empresaRepository, AuditoriaService auditoriaService,
                         PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository = empresaRepository;
        this.auditoriaService = auditoriaService;
        this.passwordEncoder = passwordEncoder;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
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

    // ── CAMBIAR CONTRASEÑA ────────────────────────────────────
    @Transactional
    public void cambiarPassword(Long usuarioId, String passwordActual, String nuevoPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual no es correcta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(nuevoPassword));
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion(usuarioId, usuario.getNombreCompleto(),
                AuditoriaService.ACCION_CAMBIAR_PASSWORD, AuditoriaService.MODULO_SEGURIDAD,
                "Usuario cambió su contraseña");
    }

    // ── ACTUALIZAR PERFIL PROPIO ──────────────────────────────
    @Transactional
    public void actualizarPerfilPropio(Long usuarioId, String nombreCompleto, String cargo) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (nombreCompleto != null && !nombreCompleto.isBlank()) usuario.setNombreCompleto(nombreCompleto);
        if (cargo != null) usuario.setCargo(cargo);
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion(usuarioId, usuario.getNombreCompleto(),
                AuditoriaService.ACCION_ACTUALIZAR_PERFIL, AuditoriaService.MODULO_PERFIL,
                "Usuario actualizó su perfil");
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosActivos() {
        return usuarioRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEstadoActivo()) && "ACTIVO".equals(u.getEstadoCuenta()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> listarUsuariosPendientes() {
        return usuarioRepository.findAll().stream()
                .filter(u -> "PENDIENTE".equals(u.getEstadoCuenta()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
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

        String datosAnteriores = null;
        String datosNuevos = null;

        try {
            datosAnteriores = objectMapper.writeValueAsString(toResponseDTO(usuario));
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo serializar datos anteriores: " + e.getMessage());
        }

        if (dto.getNombreCompleto() != null) usuario.setNombreCompleto(dto.getNombreCompleto());
        if (dto.getCargo()          != null) usuario.setCargo(dto.getCargo());
        if (dto.getFotoUrl()        != null) usuario.setFotoUrl(dto.getFotoUrl());

        if (dto.getRolId() != null) {
            Rol rol = rolRepository.findById(dto.getRolId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + dto.getRolId()));
            usuario.setRol(rol);
        }
        if (dto.getEmpresaId() != null) {
            Empresa empresa = empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + dto.getEmpresaId()));
            usuario.setEmpresa(empresa);
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPasswordHash(hashPassword(dto.getPassword()));
        }

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
        usuario.setEstadoActivo(false);
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);

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
        
        usuario.setEstadoActivo(true);
        usuario.setEstadoCuenta("ACTIVO");
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_REACTIVAR_USUARIO, AuditoriaService.MODULO_USUARIOS,
            "Reactivar usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

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

        return toResponseDTO(usuarioGuardado);
    }

    // ── APROBAR (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public UsuarioResponseDTO aprobarUsuario(Long id, Long rolId, Long empresaId) {
        return aprobarUsuario(id, rolId, empresaId, null, "Sistema", null, null);
    }

    // ── RECHAZAR solicitud pendiente ──────────────────────────
    @Transactional
    public UsuarioResponseDTO rechazarUsuario(Long id, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
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

        return toResponseDTO(usuarioGuardado);
    }

    // ── RECHAZAR (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public UsuarioResponseDTO rechazarUsuario(Long id) {
        return rechazarUsuario(id, null, "Sistema", null, null);
    }

    // ── BLOQUEAR / DESBLOQUEAR ────────────────────────────────
    @Transactional
    public UsuarioResponseDTO toggleBloquear(Long id, Long auditorUsuarioId, String auditorUsuarioNombre, String ipAddress, String userAgent) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        boolean bloquear = !Boolean.TRUE.equals(usuario.getCuentaBloqueada());
        usuario.setCuentaBloqueada(bloquear);
        usuario.setEstadoCuenta(bloquear ? "BLOQUEADO" : "ACTIVO");
        usuario.setUpdatedAt(Instant.now());
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        String accion = bloquear ? AuditoriaService.ACCION_BLOQUEAR_USUARIO : AuditoriaService.ACCION_DESBLOQUEAR_USUARIO;
        String descripcion = (bloquear ? "Bloquear" : "Desbloquear") + " usuario: " + usuario.getNombreCompleto() + " (ID: " + id + ")";
        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            accion, AuditoriaService.MODULO_USUARIOS, descripcion,
            null, null, "EXITOSO", null, ipAddress, userAgent);

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

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }
}

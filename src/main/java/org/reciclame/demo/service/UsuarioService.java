package org.reciclame.demo.service;

import org.reciclame.demo.dto.UsuarioRequestDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.model.Empresa;
import org.reciclame.demo.model.Rol;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.EmpresaRepository;
import org.reciclame.demo.repository.RolRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpresaRepository empresaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          EmpresaRepository empresaRepository) {
        this.usuarioRepository  = usuarioRepository;
        this.rolRepository = rolRepository;
        this.empresaRepository  = empresaRepository;
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByCorreoCorporativo(dto.getCorreoCorporativo()))
            throw new RuntimeException("Ya existe un usuario con ese correo corporativo.");
        if (usuarioRepository.existsByDni(dto.getDni()))
            throw new RuntimeException("Ya existe un usuario con ese DNI.");

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

        return toResponseDTO(usuarioRepository.save(usuario));
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

    // ── READ ONE ──────────────────────────────────────────────
    public UsuarioResponseDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return toResponseDTO(usuario);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

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
        return toResponseDTO(usuarioRepository.save(usuario));
    }

    // ── DELETE lógico ─────────────────────────────────────────
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        usuario.setEstadoActivo(false);
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);
    }

    // ── APROBAR solicitud pendiente ───────────────────────────
    @Transactional
    public UsuarioResponseDTO aprobarUsuario(Long id, Long rolId, Long empresaId) {
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
        return toResponseDTO(usuarioRepository.save(usuario));
    }

    // ── RECHAZAR solicitud pendiente ──────────────────────────
    @Transactional
    public UsuarioResponseDTO rechazarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setEstadoActivo(false);
        usuario.setUpdatedAt(Instant.now());
        return toResponseDTO(usuarioRepository.save(usuario));
    }

    // ── BLOQUEAR / DESBLOQUEAR ────────────────────────────────
    @Transactional
    public UsuarioResponseDTO toggleBloquear(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        boolean bloquear = !Boolean.TRUE.equals(usuario.getCuentaBloqueada());
        usuario.setCuentaBloqueada(bloquear);
        usuario.setEstadoCuenta(bloquear ? "BLOQUEADO" : "ACTIVO");
        usuario.setUpdatedAt(Instant.now());
        return toResponseDTO(usuarioRepository.save(usuario));
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
        return Integer.toHexString(password.hashCode());
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n"))
            return "\"" + val.replace("\"", "\"\"") + "\"";
        return val;
    }
}

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

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
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
        usuario.setEstadoCuenta("PENDIENTE");
        usuario.setEstadoActivo(true);
        usuario.setCuentaBloqueada(false);
        usuario.setTfaActivo(false);
        usuario.setIntentosFallidos((byte) 0);
        usuario.setCreatedAt(Instant.now());
        usuario.setUpdatedAt(Instant.now());

        return toResponseDTO(usuarioRepository.save(usuario));
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
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        usuario.setEstadoActivo(false);
        usuario.setEstadoCuenta("INACTIVO");
        usuario.setUpdatedAt(Instant.now());
        usuarioRepository.save(usuario);
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
        dto.setRolNombre(u.getRol() != null ? u.getRol().getNombre() : null);
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
}

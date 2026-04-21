package org.reciclame.demo.service;

import org.reciclame.demo.dto.RolResponseDTO;
import org.reciclame.demo.model.Rol;
import org.reciclame.demo.repository.RolRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<RolResponseDTO> listarRoles() {
        return rolRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getEstadoActivo()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public RolResponseDTO obtenerRolPorId(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
        return toResponseDTO(rol);
    }

    private RolResponseDTO toResponseDTO(Rol r) {
        RolResponseDTO dto = new RolResponseDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setDescripcion(r.getDescripcion());
        dto.setEstadoActivo(r.getEstadoActivo());
        return dto;
    }
}

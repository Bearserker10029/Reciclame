package org.reciclame.demo.service;

import org.reciclame.demo.dto.EstadosNormativaRequestDTO;
import org.reciclame.demo.dto.EstadosNormativaResponseDTO;
import org.reciclame.demo.model.EstadosNormativa;
import org.reciclame.demo.repository.EstadosNormativaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstadosNormativaService {

    private final EstadosNormativaRepository estadosNormativaRepository;

    public EstadosNormativaService(EstadosNormativaRepository estadosNormativaRepository) {
        this.estadosNormativaRepository = estadosNormativaRepository;
    }

    public List<EstadosNormativaResponseDTO> listarEstados() {
        return estadosNormativaRepository.findByEstadoActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EstadosNormativaResponseDTO> listarEstadosTodos() {
        return estadosNormativaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EstadosNormativaResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(estadosNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de normativa no encontrado con id: " + id)));
    }

    public EstadosNormativaResponseDTO crearEstado(EstadosNormativaRequestDTO dto) {
        if (dto.getNombre() != null && estadosNormativaRepository.existsByNombreIgnoreCase(dto.getNombre()))
            throw new RuntimeException("Ya existe un estado de normativa con ese nombre.");

        EstadosNormativa e = new EstadosNormativa();
        e.setNombre(dto.getNombre());
        e.setEstadoActivo(true);
        e.setCreatedAt(Instant.now());
        return toResponseDTO(estadosNormativaRepository.save(e));
    }

    public EstadosNormativaResponseDTO actualizarEstado(Long id, EstadosNormativaRequestDTO dto) {
        EstadosNormativa e = estadosNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de normativa no encontrado con id: " + id));

        if (dto.getNombre() != null) {
            if (estadosNormativaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id))
                throw new RuntimeException("Ya existe otro estado de normativa con ese nombre.");
            e.setNombre(dto.getNombre());
        }
        if (dto.getEstadoActivo() != null) e.setEstadoActivo(dto.getEstadoActivo());

        return toResponseDTO(estadosNormativaRepository.save(e));
    }

    public void eliminarEstado(Long id) {
        EstadosNormativa e = estadosNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de normativa no encontrado con id: " + id));
        e.setEstadoActivo(false);
        estadosNormativaRepository.save(e);
    }

    private EstadosNormativaResponseDTO toResponseDTO(EstadosNormativa e) {
        EstadosNormativaResponseDTO dto = new EstadosNormativaResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setEstadoActivo(e.getEstadoActivo());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}

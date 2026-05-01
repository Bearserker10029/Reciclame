package org.reciclame.demo.service;

import org.reciclame.demo.dto.TiposNormativaRequestDTO;
import org.reciclame.demo.dto.TiposNormativaResponseDTO;
import org.reciclame.demo.model.TiposNormativa;
import org.reciclame.demo.repository.TiposNormativaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TiposNormativaService {

    private final TiposNormativaRepository tiposNormativaRepository;

    public TiposNormativaService(TiposNormativaRepository tiposNormativaRepository) {
        this.tiposNormativaRepository = tiposNormativaRepository;
    }

    public List<TiposNormativaResponseDTO> listarTipos() {
        return tiposNormativaRepository.findByEstadoActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<TiposNormativaResponseDTO> listarTiposTodos() {
        return tiposNormativaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public TiposNormativaResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(tiposNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de normativa no encontrado con id: " + id)));
    }

    public TiposNormativaResponseDTO crearTipo(TiposNormativaRequestDTO dto) {
        if (dto.getNombre() != null && tiposNormativaRepository.existsByNombreIgnoreCase(dto.getNombre()))
            throw new RuntimeException("Ya existe un tipo de normativa con ese nombre.");

        TiposNormativa t = new TiposNormativa();
        t.setNombre(dto.getNombre());
        t.setEstadoActivo(true);
        t.setCreatedAt(Instant.now());
        return toResponseDTO(tiposNormativaRepository.save(t));
    }

    public TiposNormativaResponseDTO actualizarTipo(Long id, TiposNormativaRequestDTO dto) {
        TiposNormativa t = tiposNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de normativa no encontrado con id: " + id));

        if (dto.getNombre() != null) {
            if (tiposNormativaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id))
                throw new RuntimeException("Ya existe otro tipo de normativa con ese nombre.");
            t.setNombre(dto.getNombre());
        }
        if (dto.getEstadoActivo() != null) t.setEstadoActivo(dto.getEstadoActivo());

        return toResponseDTO(tiposNormativaRepository.save(t));
    }

    public void eliminarTipo(Long id) {
        TiposNormativa t = tiposNormativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de normativa no encontrado con id: " + id));
        t.setEstadoActivo(false);
        tiposNormativaRepository.save(t);
    }

    private TiposNormativaResponseDTO toResponseDTO(TiposNormativa t) {
        TiposNormativaResponseDTO dto = new TiposNormativaResponseDTO();
        dto.setId(t.getId());
        dto.setNombre(t.getNombre());
        dto.setEstadoActivo(t.getEstadoActivo());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}

package org.reciclame.demo.service;

import org.reciclame.demo.dto.TematicaResponseDTO;
import org.reciclame.demo.model.Tematica;
import org.reciclame.demo.repository.TematicaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TematicaService {

    private final TematicaRepository tematicaRepository;

    public TematicaService(TematicaRepository tematicaRepository) {
        this.tematicaRepository = tematicaRepository;
    }

    public List<TematicaResponseDTO> listarTematicas() {
        return tematicaRepository.findByEstadoActivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public TematicaResponseDTO obtenerTematicaPorId(Long id) {
        Tematica tematica = tematicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Temática no encontrada con id: " + id));
        return toResponseDTO(tematica);
    }

    private TematicaResponseDTO toResponseDTO(Tematica t) {
        TematicaResponseDTO dto = new TematicaResponseDTO();
        dto.setId(t.getId());
        dto.setCodigo(t.getCodigo());
        dto.setNombre(t.getNombre());
        dto.setColorHex(t.getColorHex());
        dto.setEstadoActivo(t.getEstadoActivo());
        return dto;
    }
}

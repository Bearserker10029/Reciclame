package org.reciclame.demo.service;

import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.repository.EstudioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstudioService {

    private final EstudioRepository estudioRepository;

    public EstudioService(EstudioRepository estudioRepository) {
        this.estudioRepository = estudioRepository;
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<EstudioResponseDTO> listarEstudios() {
        return estudioRepository.findByEstadoActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public EstudioResponseDTO obtenerEstudioPorId(Long id) {
        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));
        return toResponseDTO(estudio);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarEstudio(Long id) {
        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));
        estudio.setEstadoActivo(false);
        estudioRepository.save(estudio);
    }

    // ── HELPER ────────────────────────────────────────────────
    private EstudioResponseDTO toResponseDTO(Estudio e) {
        EstudioResponseDTO dto = new EstudioResponseDTO();
        dto.setId(e.getId());
        dto.setTitulo(e.getTitulo());
        dto.setDescripcionBreve(e.getDescripcionBreve());
        dto.setResumen(e.getResumen());
        dto.setAutor(e.getAutor());
        dto.setFechaEstudio(e.getFechaEstudio());
        dto.setAnio(e.getAnio());
        dto.setArchivoUrl(e.getArchivoUrl());
        dto.setArchivoTipoMime(e.getArchivoTipoMime());
        dto.setEsPublico(e.getEsPublico());
        dto.setVisualizacionesCount(e.getVisualizacionesCount());
        dto.setDescargasCount(e.getDescargasCount());
        dto.setEstadoActivo(e.getEstadoActivo());
        dto.setCreatedAt(e.getCreatedAt());

        if (e.getTematica() != null) {
            dto.setTematicaNombre(e.getTematica().getNombre());
            dto.setTematicaCodigo(e.getTematica().getCodigo());
            dto.setTematicaColor(e.getTematica().getColorHex());
        }

        return dto;
    }
}

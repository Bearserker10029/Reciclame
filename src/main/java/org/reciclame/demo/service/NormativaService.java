package org.reciclame.demo.service;

import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.repository.NormativaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NormativaService {

    private final NormativaRepository normativaRepository;

    public NormativaService(NormativaRepository normativaRepository) {
        this.normativaRepository = normativaRepository;
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<NormativaResponseDTO> listarNormativas() {
        return normativaRepository.findByEstadoActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public NormativaResponseDTO obtenerNormativaPorId(Long id) {
        Normativa normativa = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        return toResponseDTO(normativa);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarNormativa(Long id) {
        Normativa normativa = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        normativa.setEstadoActivo(false);
        normativaRepository.save(normativa);
    }

    // ── HELPER ────────────────────────────────────────────────
    private NormativaResponseDTO toResponseDTO(Normativa n) {
        NormativaResponseDTO dto = new NormativaResponseDTO();
        dto.setId(n.getId());
        dto.setTitulo(n.getTitulo());
        dto.setDescripcionBreve(n.getDescripcionBreve());
        dto.setAmbito(n.getAmbito());
        dto.setClasificacion(n.getClasificacion());
        dto.setAnio(n.getAnio());
        dto.setFechaPromulgacion(n.getFechaPromulgacion());
        dto.setFechaEntradaVigencia(n.getFechaEntradaVigencia());
        dto.setArchivoUrl(n.getArchivoUrl());
        dto.setEsPublica(n.getEsPublica());
        dto.setVisualizacionesCount(n.getVisualizacionesCount());
        dto.setDescargasCount(n.getDescargasCount());
        dto.setEstadoActivo(n.getEstadoActivo());
        dto.setCreatedAt(n.getCreatedAt());

        if (n.getTematica() != null) {
            dto.setTematicaNombre(n.getTematica().getNombre());
            dto.setTematicaCodigo(n.getTematica().getCodigo());
            dto.setTematicaColor(n.getTematica().getColorHex());
        }
        if (n.getTipoNormativa() != null) {
            dto.setTipoNormativaNombre(n.getTipoNormativa().getNombre());
        }
        if (n.getEstadoNormativa() != null) {
            dto.setEstadoNormativaNombre(n.getEstadoNormativa().getNombre());
        }

        return dto;
    }
}

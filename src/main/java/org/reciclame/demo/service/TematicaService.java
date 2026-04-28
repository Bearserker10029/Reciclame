package org.reciclame.demo.service;

import org.reciclame.demo.dto.TematicaRequestDTO;
import org.reciclame.demo.dto.TematicaResponseDTO;
import org.reciclame.demo.model.Tematica;
import org.reciclame.demo.repository.TematicaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public List<TematicaResponseDTO> listarTematicasTodas() {
        return tematicaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public TematicaResponseDTO obtenerTematicaPorId(Long id) {
        Tematica t = tematicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Temática no encontrada con id: " + id));
        return toResponseDTO(t);
    }

    public TematicaResponseDTO crearTematica(TematicaRequestDTO dto) {
        if (dto.getCodigo() != null && tematicaRepository.findByCodigo(dto.getCodigo().toUpperCase()).isPresent())
            throw new RuntimeException("Ya existe una temática con ese código.");

        Tematica t = new Tematica();
        t.setCodigo(dto.getCodigo() != null ? dto.getCodigo().toUpperCase() : null);
        t.setNombre(dto.getNombre());
        t.setColorHex(dto.getColorHex() != null ? dto.getColorHex() : "#95A5A6");
        t.setEstadoActivo(true);
        t.setCreatedAt(Instant.now());
        return toResponseDTO(tematicaRepository.save(t));
    }

    public TematicaResponseDTO actualizarTematica(Long id, TematicaRequestDTO dto) {
        Tematica t = tematicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Temática no encontrada con id: " + id));

        if (dto.getCodigo() != null) {
            String codigoUpper = dto.getCodigo().toUpperCase();
            tematicaRepository.findByCodigo(codigoUpper)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(x -> { throw new RuntimeException("Ya existe otra temática con ese código."); });
            t.setCodigo(codigoUpper);
        }
        if (dto.getNombre()       != null) t.setNombre(dto.getNombre());
        if (dto.getColorHex()     != null) t.setColorHex(dto.getColorHex());
        if (dto.getEstadoActivo() != null) t.setEstadoActivo(dto.getEstadoActivo());

        return toResponseDTO(tematicaRepository.save(t));
    }

    public void eliminarTematica(Long id) {
        Tematica t = tematicaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Temática no encontrada con id: " + id));
        t.setEstadoActivo(false);
        tematicaRepository.save(t);
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

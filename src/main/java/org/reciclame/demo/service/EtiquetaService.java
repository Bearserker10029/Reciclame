package org.reciclame.demo.service;

import org.reciclame.demo.dto.EtiquetaRequestDTO;
import org.reciclame.demo.dto.EtiquetaResponseDTO;
import org.reciclame.demo.model.Etiqueta;
import org.reciclame.demo.repository.EtiquetaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EtiquetaService {

    private final EtiquetaRepository etiquetaRepository;

    public EtiquetaService(EtiquetaRepository etiquetaRepository) {
        this.etiquetaRepository = etiquetaRepository;
    }

    public List<EtiquetaResponseDTO> listarEtiquetas() {
        return etiquetaRepository.findByEstadoActivoTrueOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EtiquetaResponseDTO> listarEtiquetasTodas() {
        return etiquetaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EtiquetaResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(etiquetaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etiqueta no encontrada con id: " + id)));
    }

    public EtiquetaResponseDTO crearEtiqueta(EtiquetaRequestDTO dto) {
        if (dto.getNombre() != null && etiquetaRepository.existsByNombreIgnoreCase(dto.getNombre()))
            throw new RuntimeException("Ya existe una etiqueta con ese nombre.");

        Etiqueta e = new Etiqueta();
        e.setNombre(dto.getNombre());
        e.setDescripcion(dto.getDescripcion());
        e.setEstadoActivo(true);
        e.setCreatedAt(Instant.now());
        return toResponseDTO(etiquetaRepository.save(e));
    }

    public EtiquetaResponseDTO actualizarEtiqueta(Long id, EtiquetaRequestDTO dto) {
        Etiqueta e = etiquetaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etiqueta no encontrada con id: " + id));

        if (dto.getNombre() != null) {
            if (etiquetaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre(), id))
                throw new RuntimeException("Ya existe otra etiqueta con ese nombre.");
            e.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion()  != null) e.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoActivo() != null) e.setEstadoActivo(dto.getEstadoActivo());

        return toResponseDTO(etiquetaRepository.save(e));
    }

    public void eliminarEtiqueta(Long id) {
        Etiqueta e = etiquetaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Etiqueta no encontrada con id: " + id));
        e.setEstadoActivo(false);
        etiquetaRepository.save(e);
    }

    private EtiquetaResponseDTO toResponseDTO(Etiqueta e) {
        EtiquetaResponseDTO dto = new EtiquetaResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        dto.setEstadoActivo(e.getEstadoActivo());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}

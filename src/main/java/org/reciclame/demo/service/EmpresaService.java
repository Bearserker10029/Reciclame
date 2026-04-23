package org.reciclame.demo.service;

import org.reciclame.demo.dto.EmpresaRequestDTO;
import org.reciclame.demo.dto.EmpresaResponseDTO;
import org.reciclame.demo.model.Empresa;
import org.reciclame.demo.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    // ── CREATE ────────────────────────────────────────────────
    public EmpresaResponseDTO crearEmpresa(EmpresaRequestDTO dto) {
        if (dto.getRuc() != null && empresaRepository.existsByRuc(dto.getRuc()))
            throw new RuntimeException("Ya existe una empresa con ese RUC.");

        Empresa empresa = new Empresa();
        empresa.setRazonSocial(dto.getRazonSocial());
        empresa.setRuc(dto.getRuc());
        empresa.setSector(dto.getSector());
        empresa.setEstadoActivo(true);
        empresa.setCreatedAt(Instant.now());
        empresa.setUpdatedAt(Instant.now());

        return toResponseDTO(empresaRepository.save(empresa));
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<EmpresaResponseDTO> listarEmpresas() {
        return empresaRepository.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getEstadoActivo()))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public EmpresaResponseDTO obtenerEmpresaPorId(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
        return toResponseDTO(empresa);
    }

    // ── UPDATE ────────────────────────────────────────────────
    public EmpresaResponseDTO actualizarEmpresa(Long id, EmpresaRequestDTO dto) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));

        if (dto.getRazonSocial() != null) empresa.setRazonSocial(dto.getRazonSocial());
        if (dto.getSector()      != null) empresa.setSector(dto.getSector());
        empresa.setUpdatedAt(Instant.now());

        return toResponseDTO(empresaRepository.save(empresa));
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarEmpresa(Long id) {
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada con id: " + id));
        empresa.setEstadoActivo(false);
        empresa.setUpdatedAt(Instant.now());
        empresaRepository.save(empresa);
    }

    // ── HELPER ────────────────────────────────────────────────
    private EmpresaResponseDTO toResponseDTO(Empresa e) {
        EmpresaResponseDTO dto = new EmpresaResponseDTO();
        dto.setId(e.getId());
        dto.setRazonSocial(e.getRazonSocial());
        dto.setRuc(e.getRuc());
        dto.setSector(e.getSector());
        dto.setEstadoActivo(e.getEstadoActivo());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}

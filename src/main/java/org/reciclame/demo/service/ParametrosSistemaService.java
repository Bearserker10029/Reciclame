package org.reciclame.demo.service;

import org.reciclame.demo.dto.ParametrosSistemaRequestDTO;
import org.reciclame.demo.dto.ParametrosSistemaResponseDTO;
import org.reciclame.demo.model.ParametrosSistema;
import org.reciclame.demo.repository.ParametrosSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ParametrosSistemaService {

    private final ParametrosSistemaRepository parametrosRepo;

    public ParametrosSistemaService(ParametrosSistemaRepository parametrosRepo) {
        this.parametrosRepo = parametrosRepo;
    }

    public List<ParametrosSistemaResponseDTO> listarParametros() {
        return parametrosRepo.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ParametrosSistemaResponseDTO obtenerPorId(Long id) {
        return parametrosRepo.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Parámetro no encontrado: " + id));
    }

    @Transactional
    public void actualizarParametro(Long id, ParametrosSistemaRequestDTO dto) {
        ParametrosSistema param = parametrosRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Parámetro no encontrado: " + id));
        param.setValor(dto.getValor());
        param.setDescripcion(dto.getDescripcion());
        param.setEstadoActivo(dto.getEstadoActivo() != null && dto.getEstadoActivo());
        param.setUpdatedAt(Instant.now());
        parametrosRepo.save(param);
    }

    public Integer getInteger(String clave) {
        return parametrosRepo.findByClave(clave)
                .map(ParametrosSistema::getValor)
                .map(String::trim)
                .flatMap(v -> {
                    try { return java.util.Optional.of(Integer.parseInt(v)); }
                    catch (NumberFormatException e) { return java.util.Optional.empty(); }
                })
                .orElse(null);
    }

    public Integer getIntegerOrDefault(String clave, Integer defaultValue) {
        Integer v = getInteger(clave);
        return v != null ? v : defaultValue;
    }

    public Boolean getBoolean(String clave) {
        return parametrosRepo.findByClave(clave)
                .map(ParametrosSistema::getValor)
                .map(String::trim)
                .map(String::toLowerCase)
                .map(v -> v.equals("true") || v.equals("1") || v.equals("si") || v.equals("sí"))
                .orElse(null);
    }

    public String getString(String clave) {
        return parametrosRepo.findByClave(clave)
                .map(ParametrosSistema::getValor)
                .orElse(null);
    }

    private ParametrosSistemaResponseDTO toResponseDTO(ParametrosSistema p) {
        ParametrosSistemaResponseDTO dto = new ParametrosSistemaResponseDTO();
        dto.setId(p.getId());
        dto.setClave(p.getClave());
        dto.setValor(p.getValor());
        dto.setDescripcion(p.getDescripcion());
        dto.setTipoDato(p.getTipoDato());
        dto.setEstadoActivo(p.getEstadoActivo());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}

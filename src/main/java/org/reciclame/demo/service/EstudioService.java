package org.reciclame.demo.service;

import org.reciclame.demo.dto.EstudioRequestDTO;
import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.model.*;
import org.reciclame.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstudioService {

    private final EstudioRepository           estudioRepository;
    private final EstudiosEtiquetaRepository  estudiosEtiquetaRepository;
    private final TematicaRepository          tematicaRepository;
    private final EtiquetaRepository          etiquetaRepository;
    private final NormativaRepository         normativaRepository;
    private final UsuarioRepository           usuarioRepository;

    public EstudioService(EstudioRepository estudioRepository,
                          EstudiosEtiquetaRepository estudiosEtiquetaRepository,
                          TematicaRepository tematicaRepository,
                          EtiquetaRepository etiquetaRepository,
                          NormativaRepository normativaRepository,
                          UsuarioRepository usuarioRepository) {
        this.estudioRepository          = estudioRepository;
        this.estudiosEtiquetaRepository = estudiosEtiquetaRepository;
        this.tematicaRepository         = tematicaRepository;
        this.etiquetaRepository         = etiquetaRepository;
        this.normativaRepository        = normativaRepository;
        this.usuarioRepository          = usuarioRepository;
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<EstudioResponseDTO> listarEstudios() {
        return estudioRepository.findByEstadoActivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public EstudioResponseDTO obtenerEstudioPorId(Long id) {
        return toResponseDTO(estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id)));
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public EstudioResponseDTO crearEstudio(EstudioRequestDTO dto,
                                           String archivoUrl,
                                           String archivoTipoMime,
                                           String correoAutor) {
        Tematica tematica = tematicaRepository.findById(dto.getTematicaId())
                .orElseThrow(() -> new RuntimeException("Temática no encontrada"));

        Usuario subidaPor = usuarioRepository.findByCorreoCorporativo(correoAutor)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Estudio e = new Estudio();
        mapDtoToEntity(dto, e, tematica, archivoUrl, archivoTipoMime);
        e.setSubidaPor(subidaPor);
        e.setEstadoActivo(true);
        e.setVisualizacionesCount(0);
        e.setDescargasCount(0);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());

        Estudio saved = estudioRepository.save(e);
        guardarEtiquetas(saved, dto.getEtiquetasIds());
        return toResponseDTO(saved);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public EstudioResponseDTO actualizarEstudio(Long id,
                                                EstudioRequestDTO dto,
                                                String archivoUrl,
                                                String archivoTipoMime) {
        Estudio e = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));

        Tematica tematica = tematicaRepository.findById(dto.getTematicaId())
                .orElseThrow(() -> new RuntimeException("Temática no encontrada"));

        mapDtoToEntity(dto, e, tematica, archivoUrl, archivoTipoMime);
        e.setUpdatedAt(Instant.now());

        Estudio saved = estudioRepository.save(e);

        // Reemplazar etiquetas solo si se envió la lista (incluso vacía)
        if (dto.getEtiquetasIds() != null) {
            estudiosEtiquetaRepository.deleteByEstudioId(saved.getId());
            guardarEtiquetas(saved, dto.getEtiquetasIds());
        }

        return toResponseDTO(saved);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarEstudio(Long id) {
        Estudio e = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));
        e.setEstadoActivo(false);
        e.setUpdatedAt(Instant.now());
        estudioRepository.save(e);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private void mapDtoToEntity(EstudioRequestDTO dto, Estudio e,
                                Tematica tematica,
                                String archivoUrl, String archivoTipoMime) {
        e.setTitulo(dto.getTitulo());
        e.setDescripcionBreve(dto.getDescripcionBreve());
        e.setResumen(dto.getResumen());
        e.setTematica(tematica);
        e.setAutor(dto.getAutor());
        e.setFechaEstudio(dto.getFechaEstudio());
        e.setAnio(dto.getAnio());
        e.setEsPublico(Boolean.TRUE.equals(dto.getEsPublico()));

        // Archivo: solo sobrescribir si se recibió uno nuevo
        if (archivoUrl != null) {
            e.setArchivoUrl(archivoUrl);
            e.setArchivoTipoMime(archivoTipoMime);
        }

        // Normativa relacionada (opcional)
        if (dto.getNormativaRelacionadaId() != null) {
            normativaRepository.findById(dto.getNormativaRelacionadaId())
                    .ifPresent(e::setNormativaRelacionada);
        } else {
            e.setNormativaRelacionada(null);
        }
    }

    private void guardarEtiquetas(Estudio estudio, List<Long> etiquetasIds) {
        if (etiquetasIds == null || etiquetasIds.isEmpty()) return;
        for (Long etiquetaId : etiquetasIds) {
            etiquetaRepository.findById(etiquetaId).ifPresent(etiqueta -> {
                EstudiosEtiquetaId pk = new EstudiosEtiquetaId();
                pk.setEstudioId(estudio.getId());
                pk.setEtiquetaId(etiqueta.getId());

                EstudiosEtiqueta relacion = new EstudiosEtiqueta();
                relacion.setId(pk);
                relacion.setEstudio(estudio);
                relacion.setEtiqueta(etiqueta);
                estudiosEtiquetaRepository.save(relacion);
            });
        }
    }

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
            dto.setTematicaId(e.getTematica().getId());
            dto.setTematicaNombre(e.getTematica().getNombre());
            dto.setTematicaCodigo(e.getTematica().getCodigo());
            dto.setTematicaColor(e.getTematica().getColorHex());
        }
        if (e.getNormativaRelacionada() != null) {
            dto.setNormativaRelacionadaId(e.getNormativaRelacionada().getId());
            dto.setNormativaRelacionadaTitulo(e.getNormativaRelacionada().getTitulo());
        }

        // Etiquetas
        List<EstudiosEtiqueta> relaciones = estudiosEtiquetaRepository.findByEstudioId(e.getId());
        dto.setEtiquetasIds(relaciones.stream()
                .map(r -> r.getEtiqueta().getId())
                .collect(Collectors.toList()));
        dto.setEtiquetasNombres(relaciones.stream()
                .map(r -> r.getEtiqueta().getNombre())
                .collect(Collectors.toList()));

        return dto;
    }
}

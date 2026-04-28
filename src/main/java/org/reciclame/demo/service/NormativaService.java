package org.reciclame.demo.service;

import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.model.*;
import org.reciclame.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NormativaService {

    private final NormativaRepository            normativaRepository;
    private final NormativasEtiquetaRepository   normativasEtiquetaRepository;
    private final TematicaRepository             tematicaRepository;
    private final TiposNormativaRepository       tiposNormativaRepository;
    private final EstadosNormativaRepository     estadosNormativaRepository;
    private final EtiquetaRepository             etiquetaRepository;
    private final UsuarioRepository              usuarioRepository;

    public NormativaService(NormativaRepository normativaRepository,
                            NormativasEtiquetaRepository normativasEtiquetaRepository,
                            TematicaRepository tematicaRepository,
                            TiposNormativaRepository tiposNormativaRepository,
                            EstadosNormativaRepository estadosNormativaRepository,
                            EtiquetaRepository etiquetaRepository,
                            UsuarioRepository usuarioRepository) {
        this.normativaRepository          = normativaRepository;
        this.normativasEtiquetaRepository = normativasEtiquetaRepository;
        this.tematicaRepository           = tematicaRepository;
        this.tiposNormativaRepository     = tiposNormativaRepository;
        this.estadosNormativaRepository   = estadosNormativaRepository;
        this.etiquetaRepository           = etiquetaRepository;
        this.usuarioRepository            = usuarioRepository;
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
        return toResponseDTO(normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id)));
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public NormativaResponseDTO crearNormativa(NormativaRequestDTO dto,
                                               String archivoUrl,
                                               String archivoTipoMime,
                                               String correoAutor) {
        Usuario creadaPor = usuarioRepository.findByCorreoCorporativo(correoAutor)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Normativa n = new Normativa();
        mapDtoToEntity(dto, n, archivoUrl, archivoTipoMime);
        n.setCreadaPor(creadaPor);
        n.setEstadoActivo(true);
        n.setVisualizacionesCount(0);
        n.setDescargasCount(0);
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());

        Normativa saved = normativaRepository.save(n);
        guardarEtiquetas(saved, dto.getEtiquetasIds());
        return toResponseDTO(saved);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public NormativaResponseDTO actualizarNormativa(Long id,
                                                    NormativaRequestDTO dto,
                                                    String archivoUrl,
                                                    String archivoTipoMime) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));

        mapDtoToEntity(dto, n, archivoUrl, archivoTipoMime);
        n.setUpdatedAt(Instant.now());

        Normativa saved = normativaRepository.save(n);

        if (dto.getEtiquetasIds() != null) {
            normativasEtiquetaRepository.deleteByNormativaId(saved.getId());
            guardarEtiquetas(saved, dto.getEtiquetasIds());
        }

        return toResponseDTO(saved);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarNormativa(Long id) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        n.setEstadoActivo(false);
        n.setUpdatedAt(Instant.now());
        normativaRepository.save(n);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private void mapDtoToEntity(NormativaRequestDTO dto, Normativa n,
                                String archivoUrl, String archivoTipoMime) {
        n.setTitulo(dto.getTitulo());
        n.setDescripcionBreve(dto.getDescripcionBreve());
        n.setContenidoResumido(dto.getContenidoResumido());
        n.setAmbito(dto.getAmbito() != null ? dto.getAmbito() : "NACIONAL");
        n.setClasificacion(dto.getClasificacion() != null ? dto.getClasificacion() : "VINCULANTE");
        n.setAnio(dto.getAnio());
        n.setFechaPromulgacion(dto.getFechaPromulgacion());
        n.setFechaEntradaVigencia(dto.getFechaEntradaVigencia());
        n.setFechaDerogacion(dto.getFechaDerogacion());
        n.setEsPublica(Boolean.TRUE.equals(dto.getEsPublica()));

        if (archivoUrl != null) {
            n.setArchivoUrl(archivoUrl);
            n.setArchivoTipoMime(archivoTipoMime);
        } else if (n.getArchivoUrl() == null) {
            n.setArchivoUrl("");
        }

        tematicaRepository.findById(dto.getTematicaId())
                .ifPresent(n::setTematica);
        tiposNormativaRepository.findById(dto.getTipoNormativaId())
                .ifPresent(n::setTipoNormativa);
        estadosNormativaRepository.findById(dto.getEstadoNormativaId())
                .ifPresent(n::setEstadoNormativa);
    }

    private void guardarEtiquetas(Normativa normativa, List<Long> etiquetasIds) {
        if (etiquetasIds == null || etiquetasIds.isEmpty()) return;
        for (Long etiquetaId : etiquetasIds) {
            etiquetaRepository.findById(etiquetaId).ifPresent(etiqueta -> {
                NormativasEtiquetaId pk = new NormativasEtiquetaId();
                pk.setNormativaId(normativa.getId());
                pk.setEtiquetaId(etiqueta.getId());

                NormativasEtiqueta relacion = new NormativasEtiqueta();
                relacion.setId(pk);
                relacion.setNormativa(normativa);
                relacion.setEtiqueta(etiqueta);
                normativasEtiquetaRepository.save(relacion);
            });
        }
    }

    private NormativaResponseDTO toResponseDTO(Normativa n) {
        NormativaResponseDTO dto = new NormativaResponseDTO();
        dto.setId(n.getId());
        dto.setTitulo(n.getTitulo());
        dto.setDescripcionBreve(n.getDescripcionBreve());
        dto.setContenidoResumido(n.getContenidoResumido());
        dto.setAmbito(n.getAmbito());
        dto.setClasificacion(n.getClasificacion());
        dto.setAnio(n.getAnio());
        dto.setFechaPromulgacion(n.getFechaPromulgacion());
        dto.setFechaEntradaVigencia(n.getFechaEntradaVigencia());
        dto.setFechaDerogacion(n.getFechaDerogacion());
        dto.setArchivoUrl(n.getArchivoUrl());
        dto.setArchivoTipoMime(n.getArchivoTipoMime());
        dto.setEsPublica(n.getEsPublica());
        dto.setVisualizacionesCount(n.getVisualizacionesCount());
        dto.setDescargasCount(n.getDescargasCount());
        dto.setEstadoActivo(n.getEstadoActivo());
        dto.setCreatedAt(n.getCreatedAt());

        if (n.getTematica() != null) {
            dto.setTematicaId(n.getTematica().getId());
            dto.setTematicaNombre(n.getTematica().getNombre());
            dto.setTematicaCodigo(n.getTematica().getCodigo());
            dto.setTematicaColor(n.getTematica().getColorHex());
        }
        if (n.getTipoNormativa() != null) {
            dto.setTipoNormativaId(n.getTipoNormativa().getId());
            dto.setTipoNormativaNombre(n.getTipoNormativa().getNombre());
        }
        if (n.getEstadoNormativa() != null) {
            dto.setEstadoNormativaId(n.getEstadoNormativa().getId());
            dto.setEstadoNormativaNombre(n.getEstadoNormativa().getNombre());
        }

        List<NormativasEtiqueta> relaciones = normativasEtiquetaRepository.findByNormativaId(n.getId());
        dto.setEtiquetasIds(relaciones.stream()
                .map(r -> r.getEtiqueta().getId())
                .collect(Collectors.toList()));
        dto.setEtiquetasNombres(relaciones.stream()
                .map(r -> r.getEtiqueta().getNombre())
                .collect(Collectors.toList()));

        return dto;
    }
}

package org.reciclame.demo.service;

import org.reciclame.demo.dto.EstudioRequestDTO;
import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.dto.VirusTotalVerdict;
import org.reciclame.demo.model.*;
import org.reciclame.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EstudioService {

    private final EstudioRepository           estudioRepository;
    private final EstudiosEtiquetaRepository  estudiosEtiquetaRepository;
    private final TematicaRepository          tematicaRepository;
    private final EtiquetaRepository          etiquetaRepository;
    private final NormativaRepository         normativaRepository;
    private final UsuarioRepository           usuarioRepository;
    private final MinioStorageService         minioStorageService;
    private final NotificacionService         notificacionService;
    private final AuditoriaService            auditoriaService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public EstudioService(EstudioRepository estudioRepository,
                          EstudiosEtiquetaRepository estudiosEtiquetaRepository,
                          TematicaRepository tematicaRepository,
                          EtiquetaRepository etiquetaRepository,
                          NormativaRepository normativaRepository,
                          UsuarioRepository usuarioRepository,
                          MinioStorageService minioStorageService,
                          NotificacionService notificacionService,
                          AuditoriaService auditoriaService) {
        this.estudioRepository          = estudioRepository;
        this.estudiosEtiquetaRepository = estudiosEtiquetaRepository;
        this.tematicaRepository         = tematicaRepository;
        this.etiquetaRepository         = etiquetaRepository;
        this.normativaRepository        = normativaRepository;
        this.usuarioRepository          = usuarioRepository;
        this.minioStorageService        = minioStorageService;
        this.notificacionService        = notificacionService;
        this.auditoriaService           = auditoriaService;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // java.time.Instant (JSR-310), etc.
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // ── READ ALL ──────────────────────────────────────────────
    public List<EstudioResponseDTO> listarEstudios() {
        return estudioRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EstudioResponseDTO> listarEstudiosPublicos() {
        return estudioRepository.findByEsPublicoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EstudioResponseDTO> listarEstudiosUltimos100() {
        return estudioRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EstudioResponseDTO> listarEstudiosLimit(int limite) {
        var stream = estudioRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
        if (limite > 0) stream = stream.limit(limite);
        return stream.map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<Map<String, Object>> agregarPorTematica() {
        List<EstudioResponseDTO> estudios = listarEstudios();
        return estudios.stream()
                .filter(e -> e.getTematicaNombre() != null)
                .collect(Collectors.groupingBy(EstudioResponseDTO::getTematicaNombre, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
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
                                           String correoAutor,
                                           Long auditorUsuarioId,
                                           String auditorUsuarioNombre,
                                           String ipAddress,
                                           String userAgent) {
        return crearEstudio(dto, archivoUrl, archivoTipoMime, null,
                correoAutor, auditorUsuarioId, auditorUsuarioNombre, ipAddress, userAgent);
    }

    @Transactional
    public EstudioResponseDTO crearEstudio(EstudioRequestDTO dto,
                                           String archivoUrl,
                                           String archivoTipoMime,
                                           VirusTotalVerdict vtVerdict,
                                           String correoAutor,
                                           Long auditorUsuarioId,
                                           String auditorUsuarioNombre,
                                           String ipAddress,
                                           String userAgent) {
        Tematica tematica = tematicaRepository.findById(dto.getTematicaId())
                .orElseThrow(() -> new RuntimeException("Temática no encontrada"));

        Usuario subidaPor = usuarioRepository.findByCorreoCorporativo(correoAutor)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado: " + correoAutor));

        Estudio e = new Estudio();
        mapDtoToEntity(dto, e, tematica, archivoUrl, archivoTipoMime);
        applyVerdict(e, vtVerdict);
        e.setSubidaPor(subidaPor);
        e.setEstadoActivo(true);
        e.setVisualizacionesCount(0);
        e.setDescargasCount(0);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());

        Estudio saved = estudioRepository.save(e);
        guardarEtiquetas(saved, dto.getEtiquetasIds());
        try { notificacionService.notificarNuevoEstudio(saved.getTitulo()); } catch (Exception ex) { /* no rollback */ }

        try {
            String datosNuevos = objectMapper.writeValueAsString(toResponseDTO(saved));
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
                "Crear estudio: " + saved.getTitulo(),
                null, datosNuevos, "EXITOSO", null, ipAddress, userAgent);
        } catch (Exception ex) {
            System.err.println("Error al serializar datos de auditoría: " + ex.getMessage());
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
                "Crear estudio: " + saved.getTitulo(),
                null, null, "EXITOSO", null, ipAddress, userAgent);
        }

        return toResponseDTO(saved);
    }

    // ── CREATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public EstudioResponseDTO crearEstudio(EstudioRequestDTO dto,
                                           String archivoUrl,
                                           String archivoTipoMime,
                                           String correoAutor) {
        return crearEstudio(dto, archivoUrl, archivoTipoMime, correoAutor, null, "Sistema", null, null);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public EstudioResponseDTO actualizarEstudio(Long id,
                                                EstudioRequestDTO dto,
                                                String archivoUrl,
                                                String archivoTipoMime,
                                                Long auditorUsuarioId,
                                                String auditorUsuarioNombre,
                                                String ipAddress,
                                                String userAgent) {
        return actualizarEstudio(id, dto, archivoUrl, archivoTipoMime, null,
                auditorUsuarioId, auditorUsuarioNombre, ipAddress, userAgent);
    }

    @Transactional
    public EstudioResponseDTO actualizarEstudio(Long id,
                                                EstudioRequestDTO dto,
                                                String archivoUrl,
                                                String archivoTipoMime,
                                                VirusTotalVerdict vtVerdict,
                                                Long auditorUsuarioId,
                                                String auditorUsuarioNombre,
                                                String ipAddress,
                                                String userAgent) {
        Estudio e = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));

        // Si llega un archivo nuevo y es distinto al actual, eliminar el viejo de MinIO
        // para evitar archivos huérfanos y (en combinación con el nonce del visor) forzar
        // que kkFileView sirva el archivo nuevo, no uno cacheado por fullfilename.
        if (archivoUrl != null && e.getArchivoUrl() != null
                && !archivoUrl.equals(e.getArchivoUrl())
                && !e.getArchivoUrl().isBlank()) {
            try {
                minioStorageService.eliminarArchivo(e.getArchivoUrl());
            } catch (Exception ex) {
                System.err.println("Advertencia: no se pudo eliminar archivo anterior de MinIO: " + ex.getMessage());
            }
        }

        String datosAnteriores = null;
        try {
            datosAnteriores = objectMapper.writeValueAsString(toResponseDTO(e));
        } catch (Exception ex) {
            System.err.println("Advertencia: No se pudo serializar datos anteriores: " + ex.getMessage());
        }

        Tematica tematica = tematicaRepository.findById(dto.getTematicaId())
                .orElseThrow(() -> new RuntimeException("Temática no encontrada"));

        mapDtoToEntity(dto, e, tematica, archivoUrl, archivoTipoMime);
        if (vtVerdict != null) {
            applyVerdict(e, vtVerdict);
        }
        e.setUpdatedAt(Instant.now());

        Estudio saved = estudioRepository.save(e);

        // Reemplazar etiquetas solo si se envió la lista (incluso vacía)
        if (dto.getEtiquetasIds() != null) {
            estudiosEtiquetaRepository.deleteByEstudioId(saved.getId());
            guardarEtiquetas(saved, dto.getEtiquetasIds());
        }

        try { notificacionService.notificarEstudioActualizado(saved.getTitulo()); } catch (Exception ex) { /* no rollback */ }

        String datosNuevos = null;
        try {
            datosNuevos = objectMapper.writeValueAsString(toResponseDTO(saved));
        } catch (Exception ex) {
            System.err.println("Advertencia: No se pudo serializar datos nuevos: " + ex.getMessage());
        }

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_EDITAR_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
            "Editar estudio: " + saved.getTitulo() + " (ID: " + id + ")",
            datosAnteriores, datosNuevos, "EXITOSO", null, ipAddress, userAgent);

        return toResponseDTO(saved);
    }

    // ── UPDATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public EstudioResponseDTO actualizarEstudio(Long id,
                                                EstudioRequestDTO dto,
                                                String archivoUrl,
                                                String archivoTipoMime) {
        return actualizarEstudio(id, dto, archivoUrl, archivoTipoMime, null, "Sistema", null, null);
    }

    // ── VISUALIZACIONES ───────────────────────────────────────
    @Transactional
    public int incrementarVisualizaciones(Long id) {
        Estudio estudio = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado: " + id));
        int nuevo = (estudio.getVisualizacionesCount() != null ? estudio.getVisualizacionesCount() : 0) + 1;
        estudio.setVisualizacionesCount(nuevo);
        estudioRepository.save(estudio);
        return nuevo;
    }

    // ── DESCARGAS ─────────────────────────────────────────────
    /**
     * Incrementa el contador de descargas del estudio en +1.
     * Si el estudio no existe, la operación se ignora silenciosamente
     * (la descarga se completó correctamente; el contador es secundario).
     */
    @Transactional
    public void incrementarDescargas(Long id) {
        estudioRepository.findById(id).ifPresent(e -> {
            int nuevo = (e.getDescargasCount() != null ? e.getDescargasCount() : 0) + 1;
            e.setDescargasCount(nuevo);
            estudioRepository.save(e);
        });
    }

    public EstudioResponseDTO obtenerEstudioPublicoPorId(Long id) {
        EstudioResponseDTO dto = obtenerEstudioPorId(id);
        if (!Boolean.TRUE.equals(dto.getEsPublico())) {
            throw new RuntimeException("Este estudio no está disponible públicamente.");
        }
        return dto;
    }

    // ── DELETE físico ─────────────────────────────────────────
    @Transactional
    public void eliminarEstudio(Long id,
                                Long auditorUsuarioId,
                                String auditorUsuarioNombre,
                                String ipAddress,
                                String userAgent) {
        Estudio e = estudioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudio no encontrado con id: " + id));
        String archivoUrl = e.getArchivoUrl();

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_ELIMINAR_ESTUDIO, AuditoriaService.MODULO_ESTUDIOS,
            "Eliminar estudio: " + e.getTitulo() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

        // Eliminar etiquetas de la tabla intermedia antes de borrar el registro
        estudiosEtiquetaRepository.deleteByEstudioId(id);
        estudioRepository.deleteById(id);

        // Eliminar archivo de MinIO (fuera de la transacción JPA, no revierte el DELETE)
        minioStorageService.eliminarArchivo(archivoUrl);
    }

    // ── DELETE (overload sin auditoría para compatibilidad) ────────────
    @Transactional
    public void eliminarEstudio(Long id) {
        eliminarEstudio(id, null, "Sistema", null, null);
    }

    public long contarNuevosDesde(Instant desde) {
        return estudioRepository.contarNuevosDesde(desde);
    }

    public long contarEntreFechas(Instant desde, Instant hasta) {
        return estudioRepository.contarEntreFechas(desde, hasta);
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

    private void applyVerdict(Estudio e, VirusTotalVerdict v) {
        if (v == null) return;
        e.setVtSha256(v.getSha256());
        e.setVtVerdict(v.getVerdict() != null ? v.getVerdict().name() : null);
        e.setVtMaliciousEngines(v.getMaliciousEngines());
        e.setVtSuspiciousEngines(v.getSuspiciousEngines());
        e.setVtTotalEngines(v.getTotalEngines());
        e.setVtSkipped(Boolean.TRUE.equals(v.isSkipped()));
        e.setVtScannedAt(v.getScannedAt());
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

        dto.setVtSha256(e.getVtSha256());
        dto.setVtVerdict(e.getVtVerdict());
        dto.setVtMaliciousEngines(e.getVtMaliciousEngines());
        dto.setVtSuspiciousEngines(e.getVtSuspiciousEngines());
        dto.setVtTotalEngines(e.getVtTotalEngines());
        dto.setVtSkipped(e.getVtSkipped());
        dto.setVtScannedAt(e.getVtScannedAt());

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

        // Etiquetas (defensivo: el repo puede devolver null o filas sin etiqueta)
        List<EstudiosEtiqueta> relaciones = estudiosEtiquetaRepository.findByEstudioId(e.getId());
        if (relaciones == null) relaciones = Collections.emptyList();
        dto.setEtiquetasIds(relaciones.stream()
                .filter(r -> r != null && r.getEtiqueta() != null && r.getEtiqueta().getId() != null)
                .map(r -> r.getEtiqueta().getId())
                .collect(Collectors.toList()));
        dto.setEtiquetasNombres(relaciones.stream()
                .filter(r -> r != null && r.getEtiqueta() != null && r.getEtiqueta().getNombre() != null)
                .map(r -> r.getEtiqueta().getNombre())
                .collect(Collectors.toList()));

        return dto;
    }
}

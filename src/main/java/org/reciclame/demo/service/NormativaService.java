package org.reciclame.demo.service;

import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.dto.VirusTotalVerdict;
import org.reciclame.demo.helper.EstadoValidacion;
import org.reciclame.demo.model.*;
import org.reciclame.demo.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

@Service
public class NormativaService {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket}")
    private String minioBucket;

    private final NormativaRepository            normativaRepository;
    private final NormativasEtiquetaRepository   normativasEtiquetaRepository;
    private final TematicaRepository             tematicaRepository;
    private final TiposNormativaRepository       tiposNormativaRepository;
    private final EstadosNormativaRepository     estadosNormativaRepository;
    private final EtiquetaRepository             etiquetaRepository;
    private final UsuarioRepository              usuarioRepository;
    private final NotificacionService            notificacionService;
    private final AuditoriaService               auditoriaService;
    private final MinioStorageService            minioStorageService;
    private final MinioPendingDeletionService    minioPendingDeletionService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public NormativaService(NormativaRepository normativaRepository,
                            NormativasEtiquetaRepository normativasEtiquetaRepository,
                            TematicaRepository tematicaRepository,
                            TiposNormativaRepository tiposNormativaRepository,
                            EstadosNormativaRepository estadosNormativaRepository,
                            EtiquetaRepository etiquetaRepository,
                            UsuarioRepository usuarioRepository,
                            NotificacionService notificacionService,
                            AuditoriaService auditoriaService,
                            MinioStorageService minioStorageService,
                            MinioPendingDeletionService minioPendingDeletionService) {
        this.normativaRepository           = normativaRepository;
        this.normativasEtiquetaRepository  = normativasEtiquetaRepository;
        this.tematicaRepository            = tematicaRepository;
        this.tiposNormativaRepository      = tiposNormativaRepository;
        this.estadosNormativaRepository    = estadosNormativaRepository;
        this.etiquetaRepository            = etiquetaRepository;
        this.usuarioRepository             = usuarioRepository;
        this.notificacionService           = notificacionService;
        this.auditoriaService              = auditoriaService;
        this.minioStorageService           = minioStorageService;
        this.minioPendingDeletionService   = minioPendingDeletionService;
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // java.time.Instant (JSR-310), etc.
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    // ── READ ALL ──────────────────────────────────────────────

    /**
     * Para selectores (estudios-form): devuelve las normativas cuyo archivo está almacenado
     * en el contenedor MinIO configurado, ordenadas por título.
     * Excluye normativas con URLs de proveedores externos (GCP, S3, etc.) para garantizar
     * consistencia entre la BD y el almacenamiento del contenedor.
     */
    public List<NormativaResponseDTO> listarNormativasParaSelector() {
        String minioPathMarker = "/" + minioBucket + "/";
        return normativaRepository.findAll(Sort.by("titulo")).stream()
                .filter(n -> n.getArchivoUrl() != null && n.getArchivoUrl().contains(minioPathMarker))
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<NormativaResponseDTO> listarNormativas() {
        return normativaRepository.findByEstadoActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<NormativaResponseDTO> listarNormativasUltimas100() {
        return normativaRepository.findTop100ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<NormativaResponseDTO> listarNormativasLimit(int limite) {
        var stream = normativaRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
        if (limite > 0) stream = stream.limit(limite);
        return stream.map(this::toResponseDTO).collect(Collectors.toList());
    }

    public Map<String, Long> contarPorTematica() {
        // JDK 22+ Stream Gatherers: fold mantiene un acumulador HashMap y lo
        // emite al cerrar el stream. Reemplaza el patrón manual getOrDefault.
        return normativaRepository.findByEstadoActivoTrue().stream()
                .gather(Gatherers.fold(
                        HashMap<String, Long>::new,
                        (acc, n) -> {
                            String codigo = n.getTematica() != null && n.getTematica().getCodigo() != null
                                    ? n.getTematica().getCodigo()
                                    : "Otro";
                            acc.merge(codigo, 1L, Long::sum);
                            return acc;
                        }))
                .findFirst()
                .orElseGet(HashMap::new);
    }

    public List<Map<String, Object>> agregarPorTematica() {
        List<Normativa> activas = normativaRepository.findByEstadoActivoTrue();
        return activas.stream()
                .filter(n -> n.getTematica() != null && n.getTematica().getNombre() != null)
                .collect(Collectors.groupingBy(n -> n.getTematica().getNombre(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", e.getKey());
                    item.put("value", e.getValue().intValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<NormativaResponseDTO> listarNormativasPublicas() {
        return normativaRepository.findByEstadoActivoTrueAndEsPublicaTrue()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<NormativaResponseDTO> topMasVistas() {
        return normativaRepository.findTop5ByEstadoActivoTrueAndEsPublicaTrueOrderByVisualizacionesCountDesc()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ── READ ONE ──────────────────────────────────────────────
    public NormativaResponseDTO obtenerNormativaPorId(Long id) {
        return toResponseDTO(normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id)));
    }

    public NormativaResponseDTO obtenerNormativaPublicaPorId(Long id) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        if (!Boolean.TRUE.equals(n.getEsPublica())) {
            throw new RuntimeException("Esta normativa no es de acceso público");
        }
        return toResponseDTO(n);
    }

    // ── DESCARGAS ─────────────────────────────────────────────
    /**
     * Incrementa el contador de descargas de la normativa en +1.
     * Si la normativa no existe, la operación se ignora silenciosamente.
     */
    @Transactional
    public void incrementarDescargas(Long id) {
        normativaRepository.findById(id).ifPresent(n -> {
            int nuevo = (n.getDescargasCount() != null ? n.getDescargasCount() : 0) + 1;
            n.setDescargasCount(nuevo);
            normativaRepository.save(n);
        });
    }

    // ── CREATE ────────────────────────────────────────────────
    @Transactional
    public NormativaResponseDTO crearNormativa(NormativaRequestDTO dto,
                                               String archivoUrl,
                                               String archivoTipoMime,
                                               String correoAutor,
                                               Long auditorUsuarioId,
                                               String auditorUsuarioNombre,
                                               String ipAddress,
                                               String userAgent) {
        return crearNormativa(dto, archivoUrl, archivoTipoMime, null,
                correoAutor, auditorUsuarioId, auditorUsuarioNombre, ipAddress, userAgent);
    }

    @Transactional
    public NormativaResponseDTO crearNormativa(NormativaRequestDTO dto,
                                               String archivoUrl,
                                               String archivoTipoMime,
                                               VirusTotalVerdict vtVerdict,
                                               String correoAutor,
                                               Long auditorUsuarioId,
                                               String auditorUsuarioNombre,
                                               String ipAddress,
                                               String userAgent) {
        Usuario creadaPor = usuarioRepository.findByCorreoCorporativo(correoAutor)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Normativa n = new Normativa();
        mapDtoToEntity(dto, n, archivoUrl, archivoTipoMime);
        applyVerdict(n, vtVerdict);
        n.setCreadaPor(creadaPor);
        n.setEstadoActivo(true);
        n.setVisualizacionesCount(0);
        n.setDescargasCount(0);
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());

        Normativa saved = normativaRepository.save(n);
        guardarEtiquetas(saved, dto.getEtiquetasIds());
        try { notificacionService.notificarNuevaNormativa(saved.getTitulo()); } catch (Exception ex) { /* no rollback */ }

        try {
            String datosNuevos = objectMapper.writeValueAsString(toResponseDTO(saved));
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                "Crear normativa: " + saved.getTitulo(),
                null, datosNuevos, "EXITOSO", null, ipAddress, userAgent);
        } catch (Exception e) {
            System.err.println("Error al serializar datos de auditoría: " + e.getMessage());
            auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                "Crear normativa: " + saved.getTitulo(),
                null, null, "EXITOSO", null, ipAddress, userAgent);
        }

        return toResponseDTO(saved);
    }

    /**
     * Crea una Normativa en estado PENDIENTE de validación VirusTotal. Se usa como
     * paso 1 del flujo de subida asíncrona: el archivo YA está en el bucket staging
     * de MinIO, y el worker {@code UploadStagingScanner} terminará el trabajo (scan +
     * promote) en background.
     * <p>
     * Diferencias con {@link #crearNormativa}:
     * <ul>
     *   <li>No dispara notificaciones (se hace al aprobar).</li>
     *   <li>Auditoría usa acción dedicada para distinguir "subida pendiente".</li>
     *   <li>{@code archivoUrl} debe ser el marker de staging ({@code staging://<uuid>}).
     *       El worker lo actualizará a la URL definitiva (o lo dejará en staging si VT rechaza).</li>
     * </ul>
     */
    @Transactional
    public NormativaResponseDTO crearNormativaPendiente(NormativaRequestDTO dto,
                                                        String archivoStagingUrl,
                                                        String archivoTipoMime,
                                                        String correoAutor,
                                                        Long auditorUsuarioId,
                                                        String auditorUsuarioNombre,
                                                        String ipAddress,
                                                        String userAgent) {
        if (archivoStagingUrl == null || !archivoStagingUrl.startsWith("staging://")) {
            throw new RuntimeException("crearNormativaPendiente requiere archivoStagingUrl con marker staging://");
        }
        Usuario creadaPor = usuarioRepository.findByCorreoCorporativo(correoAutor)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado"));

        Normativa n = new Normativa();
        mapDtoToEntity(dto, n, archivoStagingUrl, archivoTipoMime);
        n.setCreadaPor(creadaPor);
        n.setEstadoActivo(true);
        n.setVisualizacionesCount(0);
        n.setDescargasCount(0);
        n.setCreatedAt(Instant.now());
        n.setUpdatedAt(Instant.now());

        Normativa saved = normativaRepository.save(n);
        guardarEtiquetas(saved, dto.getEtiquetasIds());

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                "Subida de normativa pendiente de validación: " + saved.getTitulo() + " (staging=" + archivoStagingUrl + ")",
                null, null, "PENDIENTE", null, ipAddress, userAgent);

        return toResponseDTO(saved);
    }

    /**
     * Actualiza el registro tras finalizar el escaneo VirusTotal.
     * Llamado por {@code UploadStagingScanner} en background.
     *
     * @param id               ID de la Normativa.
     * @param vtVerdict        Veredicto final (puede ser null si hubo error técnico).
     * @param archivoUrlFinal  URL definitiva del archivo en MinIO docs
     *                         (o el mismo {@code staging://<uuid>} si VT rechazó / error).
     * @param auditorUsuarioId/...  Datos de auditoría.
     */
    @Transactional
    public void actualizarEstadoValidacion(Long id,
                                           VirusTotalVerdict vtVerdict,
                                           String archivoUrlFinal,
                                           Long auditorUsuarioId,
                                           String auditorUsuarioNombre,
                                           String ipAddress,
                                           String userAgent) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        applyVerdict(n, vtVerdict);
        if (archivoUrlFinal != null) {
            n.setArchivoUrl(archivoUrlFinal);
            n.setArchivoTipoMime(archivoUrlFinal.startsWith("staging://")
                    ? n.getArchivoTipoMime()
                    : n.getArchivoTipoMime());
        }
        n.setUpdatedAt(Instant.now());
        normativaRepository.save(n);

        String accion;
        String resultado;
        if (vtVerdict == null || vtVerdict.getVerdict() == org.reciclame.demo.dto.VirusTotalVerdict.Verdict.SKIPPED) {
            accion = "Validación de archivo finalizada (SKIPPED)";
            resultado = "EXITOSO";
        } else if (vtVerdict.getVerdict() == org.reciclame.demo.dto.VirusTotalVerdict.Verdict.MALICIOUS) {
            accion = "Archivo rechazado por antivirus";
            resultado = "RECHAZADO";
            // DESACTIVAR el registro para que no sea visible (archivo bloqueado).
            n.setEstadoActivo(false);
            normativaRepository.save(n);
        } else {
            accion = "Validación de archivo finalizada (" + vtVerdict.getVerdict() + ")";
            resultado = "EXITOSO";
        }
        String detalle = vtVerdict != null ? vtVerdict.getMessage() : null;
        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
                AuditoriaService.ACCION_CREAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
                accion + " — " + n.getTitulo() + " (ID " + n.getId() + ")",
                null, null, resultado, detalle, ipAddress, userAgent);

        // Notificar al admin/socios SOLO si el archivo se promovió exitosamente.
        if (resultado.equals("EXITOSO")
                && vtVerdict != null
                && vtVerdict.getVerdict() != org.reciclame.demo.dto.VirusTotalVerdict.Verdict.SKIPPED) {
            try { notificacionService.notificarNuevaNormativa(n.getTitulo()); } catch (Exception ignored) {}
        }
    }

    // ── CREATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public NormativaResponseDTO crearNormativa(NormativaRequestDTO dto,
                                               String archivoUrl,
                                               String archivoTipoMime,
                                               String correoAutor) {
        return crearNormativa(dto, archivoUrl, archivoTipoMime, correoAutor, null, "Sistema", null, null);
    }

    // ── UPDATE ────────────────────────────────────────────────
    @Transactional
    public NormativaResponseDTO actualizarNormativa(Long id,
                                                    NormativaRequestDTO dto,
                                                    String archivoUrl,
                                                    String archivoTipoMime,
                                                    Long auditorUsuarioId,
                                                    String auditorUsuarioNombre,
                                                    String ipAddress,
                                                    String userAgent) {
        return actualizarNormativa(id, dto, archivoUrl, archivoTipoMime, null,
                auditorUsuarioId, auditorUsuarioNombre, ipAddress, userAgent);
    }

    @Transactional
    public NormativaResponseDTO actualizarNormativa(Long id,
                                                    NormativaRequestDTO dto,
                                                    String archivoUrl,
                                                    String archivoTipoMime,
                                                    VirusTotalVerdict vtVerdict,
                                                    Long auditorUsuarioId,
                                                    String auditorUsuarioNombre,
                                                    String ipAddress,
                                                    String userAgent) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));

        // Si llega un archivo nuevo y es distinto al actual, programar purga del viejo
        // en MinIO con 72h de gracia. El archivo viejo NO se borra inmediato: si el
        // reemplazo fue accidental, hay margen para recuperarlo. El nonce del visor
        // sigue siendo lo que fuerza a kkFileView a servir el archivo nuevo, no uno
        // cacheado por fullfilename.
        if (archivoUrl != null && n.getArchivoUrl() != null
                && !archivoUrl.equals(n.getArchivoUrl())
                && !n.getArchivoUrl().isBlank()) {
            try {
                String objectName = minioStorageService.getObjectNameFromUrl(n.getArchivoUrl());
                minioPendingDeletionService.programarEliminacion(
                        n.getArchivoUrl(),
                        objectName,
                        MinioPendingDeletion.TIPO_NORMATIVA,
                        id,
                        MinioPendingDeletion.MOTIVO_REEMPLAZO);
            } catch (Exception ex) {
                System.err.println("Advertencia: no se pudo programar purga de archivo anterior de MinIO: " + ex.getMessage());
            }
        }

        String datosAnteriores = null;
        try {
            datosAnteriores = objectMapper.writeValueAsString(toResponseDTO(n));
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo serializar datos anteriores: " + e.getMessage());
        }

        mapDtoToEntity(dto, n, archivoUrl, archivoTipoMime);
        if (vtVerdict != null) {
            // sólo refrescamos los campos VT si llega archivo nuevo (veredicto presente)
            applyVerdict(n, vtVerdict);
        }
        n.setUpdatedAt(Instant.now());

        Normativa saved = normativaRepository.save(n);

        if (dto.getEtiquetasIds() != null) {
            normativasEtiquetaRepository.deleteByNormativaId(saved.getId());
            guardarEtiquetas(saved, dto.getEtiquetasIds());
        }

        try { notificacionService.notificarNormativaActualizada(saved.getTitulo()); } catch (Exception ex) { /* no rollback */ }

        String datosNuevos = null;
        try {
            datosNuevos = objectMapper.writeValueAsString(toResponseDTO(saved));
        } catch (Exception e) {
            System.err.println("Advertencia: No se pudo serializar datos nuevos: " + e.getMessage());
        }

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_EDITAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
            "Editar normativa: " + saved.getTitulo() + " (ID: " + id + ")",
            datosAnteriores, datosNuevos, "EXITOSO", null, ipAddress, userAgent);

        return toResponseDTO(saved);
    }

    // ── UPDATE (overload sin auditoría para compatibilidad) ────────────────
    @Transactional
    public NormativaResponseDTO actualizarNormativa(Long id,
                                                    NormativaRequestDTO dto,
                                                    String archivoUrl,
                                                    String archivoTipoMime) {
        return actualizarNormativa(id, dto, archivoUrl, archivoTipoMime, null, "Sistema", null, null);
    }

    // ── DELETE lógico ─────────────────────────────────────────
    public void eliminarNormativa(Long id,
                                  Long auditorUsuarioId,
                                  String auditorUsuarioNombre,
                                  String ipAddress,
                                  String userAgent) {
        Normativa n = normativaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Normativa no encontrada con id: " + id));
        String archivoUrl = n.getArchivoUrl();
        n.setEstadoActivo(false);
        n.setUpdatedAt(Instant.now());
        normativaRepository.save(n);

        auditoriaService.registrarAccion(auditorUsuarioId, auditorUsuarioNombre,
            AuditoriaService.ACCION_ELIMINAR_NORMATIVA, AuditoriaService.MODULO_NORMATIVAS,
            "Eliminar normativa: " + n.getTitulo() + " (ID: " + id + ")",
            null, null, "EXITOSO", null, ipAddress, userAgent);

        // Programar purga del archivo de MinIO con 72h de gracia. Igual que en el
        // update, esto da margen para revertir la eliminación si fue un error.
        if (archivoUrl != null && !archivoUrl.isBlank()) {
            try {
                String objectName = minioStorageService.getObjectNameFromUrl(archivoUrl);
                minioPendingDeletionService.programarEliminacion(
                        archivoUrl,
                        objectName,
                        MinioPendingDeletion.TIPO_NORMATIVA,
                        id,
                        MinioPendingDeletion.MOTIVO_ELIMINACION);
            } catch (Exception ex) {
                System.err.println("Advertencia: no se pudo programar purga de archivo de MinIO tras eliminar normativa: " + ex.getMessage());
            }
        }
    }

    // ── DELETE (overload sin auditoría para compatibilidad) ────────────
    public void eliminarNormativa(Long id) {
        eliminarNormativa(id, null, "Sistema", null, null);
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

    private void applyVerdict(Normativa n, VirusTotalVerdict v) {
        if (v == null) return;
        n.setVtSha256(v.getSha256());
        n.setVtVerdict(v.getVerdict() != null ? v.getVerdict().name() : null);
        n.setVtMaliciousEngines(v.getMaliciousEngines());
        n.setVtSuspiciousEngines(v.getSuspiciousEngines());
        n.setVtTotalEngines(v.getTotalEngines());
        n.setVtSkipped(Boolean.TRUE.equals(v.isSkipped()));
        n.setVtScannedAt(v.getScannedAt());
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

        dto.setVtSha256(n.getVtSha256());
        dto.setVtVerdict(n.getVtVerdict());
        dto.setVtMaliciousEngines(n.getVtMaliciousEngines());
        dto.setVtSuspiciousEngines(n.getVtSuspiciousEngines());
        dto.setVtTotalEngines(n.getVtTotalEngines());
        dto.setVtSkipped(n.getVtSkipped());
        dto.setVtScannedAt(n.getVtScannedAt());

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

        dto.setEstadoValidacion(EstadoValidacion.inferir(n.getArchivoUrl(), n.getVtVerdict()));

        return dto;
    }
}

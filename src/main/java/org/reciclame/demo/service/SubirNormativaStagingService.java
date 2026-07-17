package org.reciclame.demo.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Fachada que orquesta la subida de archivos para Normativa en el flujo NUEVO
 * (caché staging + validación async VirusTotal).
 *
 * <p>Responsabilidades:</p>
 * <ol>
 *   <li>Validar la extensión del archivo subido.</li>
 *   <li>Subir el archivo al bucket staging de MinIO.</li>
 *   <li>Insertar la fila en MySQL con archivo_url = staging://<uuid>, estado PENDIENTE.</li>
 *   <li>Encolar {@link UploadStagingScanner#procesarAsync} en el pool async.</li>
 *   <li>Devolver al controller el {@code (taskId, refId)} para la respuesta 202.</li>
 * </ol>
 *
 * <p>Es un wrapper liviano porque {@link UploadStagingService} ya sabe operar contra
 * MinIO staging, y {@link NormativaService.crearNormativaPendiente} ya hace el INSERT.</p>
 */
@Service
@RequiredArgsConstructor
public class SubirNormativaStagingService {

    private static final Set<String> EXTENSIONES_NORMATIVA = Set.of("pdf", "doc", "docx");

    private final UploadStagingService stagingService;
    private final NormativaService normativaService;
    private final UploadStagingScanner scanner;

    @Getter
    public static class ResultadoSubida {
        private final String taskId;
        private final Long refId;
        public ResultadoSubida(String taskId, Long refId) {
            this.taskId = taskId;
            this.refId = refId;
        }
    }

    public void validarExtension(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.contains(".")) {
            throw new IllegalArgumentException("El archivo debe ser PDF, DOC o DOCX.");
        }
        String ext = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_NORMATIVA.contains(ext)) {
            throw new IllegalArgumentException(
                    "Formato no permitido para normativas. Solo se aceptan PDF, DOC y DOCX.");
        }
    }

    public ResultadoSubida subirYPendientarNormativa(NormativaRequestDTO dto,
                                                     MultipartFile archivo,
                                                     String correoAutor,
                                                     Long auditorId,
                                                     String auditorNombre,
                                                     String ip,
                                                     String userAgent) throws IOException {
        validarExtension(archivo);
        String uuid = UUID.randomUUID().toString();
        String stagingMarker;
        try {
            stagingMarker = stagingService.put(uuid, archivo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo al bucket staging: " + e.getMessage(), e);
        }
        NormativaResponseDTO resp;
        try {
            resp = normativaService.crearNormativaPendiente(
                    dto, stagingMarker, archivo.getContentType(),
                    correoAutor, auditorId, auditorNombre, ip, userAgent);
        } catch (RuntimeException e) {
            // Rollback del staging si el INSERT falla.
            stagingService.deleteByObjectName(uuid + "_"
                    + (archivo.getOriginalFilename() != null
                        ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "documento"));
            throw e;
        }
        scanner.procesarAsync("NORMATIVA", resp.getId(), stagingMarker, archivo.getContentType(), correoAutor, null);
        return new ResultadoSubida(uuid, resp.getId());
    }

    /**
     * Para PUT con archivo nuevo: crea el registro nuevo apuntando al staging y
     * conserva el registro viejo hasta que el scanner confirme que el reemplazo
     * es seguro (ver {@link UploadStagingScanner}) — antes se borraba de entrada,
     * lo que perdía el documento si el archivo nuevo resultaba malicioso.
     */
    public ResultadoSubida subirYPendientarNormativaEnUpdate(Long id,
                                                            NormativaRequestDTO dto,
                                                            MultipartFile archivo,
                                                            String correoAutor,
                                                            Long auditorId,
                                                            String auditorNombre,
                                                            String ip,
                                                            String userAgent) throws IOException {
        validarExtension(archivo);
        String uuid = UUID.randomUUID().toString();
        String stagingMarker;
        try {
            stagingMarker = stagingService.put(uuid, archivo);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo al bucket staging: " + e.getMessage(), e);
        }
        NormativaResponseDTO resp = normativaService.crearNormativaPendiente(
                dto, stagingMarker, archivo.getContentType(),
                correoAutor, auditorId, auditorNombre, ip, userAgent);
        scanner.procesarAsync("NORMATIVA", resp.getId(), stagingMarker, archivo.getContentType(), correoAutor, id);
        return new ResultadoSubida(uuid, resp.getId());
    }
}

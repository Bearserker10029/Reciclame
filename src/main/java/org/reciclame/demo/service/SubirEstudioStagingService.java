package org.reciclame.demo.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.reciclame.demo.dto.EstudioRequestDTO;
import org.reciclame.demo.dto.EstudioResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Fachada equivalente a {@link SubirNormativaStagingService} para el flujo asíncrono
 * de subida de Estudios.
 *
 * <p>Bug colateral arreglado: el controller antiguo de Estudio NO validaba extensión.
 * Ahora se aceptan: pdf, pptx, ppt, docx, xlsx.</p>
 */
@Service
@RequiredArgsConstructor
public class SubirEstudioStagingService {

    private static final Set<String> EXTENSIONES_ESTUDIO = Set.of("pdf", "ppt", "pptx", "doc", "docx", "xlsx");

    private final UploadStagingService stagingService;
    private final EstudioService estudioService;
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
            throw new IllegalArgumentException("El archivo debe ser PDF, PPT, DOC, DOCX o XLSX.");
        }
        String ext = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES_ESTUDIO.contains(ext)) {
            throw new IllegalArgumentException(
                    "Formato no permitido para estudios. Solo se aceptan PDF, PPT, DOC, DOCX y XLSX.");
        }
    }

    public ResultadoSubida subirYPendientarEstudio(EstudioRequestDTO dto,
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
        EstudioResponseDTO resp;
        try {
            resp = estudioService.crearEstudioPendiente(
                    dto, stagingMarker, archivo.getContentType(),
                    correoAutor, auditorId, auditorNombre, ip, userAgent);
        } catch (RuntimeException e) {
            stagingService.deleteByObjectName(uuid + "_"
                    + (archivo.getOriginalFilename() != null
                        ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "documento"));
            throw e;
        }
        scanner.procesarAsync("ESTUDIO", resp.getId(), stagingMarker, archivo.getContentType(), correoAutor, null);
        return new ResultadoSubida(uuid, resp.getId());
    }

    public ResultadoSubida subirYPendientarEstudioEnUpdate(Long id,
                                                          EstudioRequestDTO dto,
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
        // El estudio anterior (id) NO se borra acá: se crea el nuevo en PENDIENTE
        // y el viejo se elimina recién cuando el scanner confirme que el archivo
        // nuevo es seguro y ya fue promovido (ver UploadStagingScanner). Si el
        // archivo resulta malicioso, el estudio original queda intacto y publicado.
        EstudioResponseDTO resp = estudioService.crearEstudioPendiente(
                dto, stagingMarker, archivo.getContentType(),
                correoAutor, auditorId, auditorNombre, ip, userAgent);
        scanner.procesarAsync("ESTUDIO", resp.getId(), stagingMarker, archivo.getContentType(), correoAutor, id);
        return new ResultadoSubida(uuid, resp.getId());
    }
}

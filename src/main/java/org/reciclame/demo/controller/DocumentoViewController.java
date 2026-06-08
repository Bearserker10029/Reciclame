package org.reciclame.demo.controller;

import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.DocumentPreviewService;
import org.reciclame.demo.service.MinioStorageService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoViewController {

    private static final int EXPIRY_MINUTOS = 15;

    private final MinioStorageService minioStorageService;
    private final DocumentPreviewService documentPreviewService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public DocumentoViewController(MinioStorageService minioStorageService,
                                   DocumentPreviewService documentPreviewService,
                                   AuditoriaService auditoriaService,
                                   UsuarioService usuarioService) {
        this.minioStorageService = minioStorageService;
        this.documentPreviewService = documentPreviewService;
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    /**
     * Genera una URL presignada temporal y redirige al cliente a ella.
     *
     * @param url URL pública almacenada en BD (http://localhost:9000/reciclame-docs/...)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/ver")
    public ResponseEntity<?> verDocumento(@RequestParam("url") String url) {
        try {
            String firmadaInterna = minioStorageService.generarUrlFirmadaInterna(url, EXPIRY_MINUTOS);
            String firmada = firmadaInterna.replace(
                    minioStorageService.getMinioInternalUrl(),
                    minioStorageService.getMinioUrl());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, firmada)
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "URL de documento inválida."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("mensaje", "No se pudo generar el enlace de acceso."));
        }
    }

    /**
     * Sirve el archivo inline desde Spring para poder embeberlo en iframes del mismo origen.
     * Usa la URL interna firmada de MinIO para que el contenedor de la app alcance al contenedor MinIO.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/inline")
    public void verDocumentoInline(@RequestParam("url") String url,
                                   HttpServletResponse response) throws IOException {
        final String firmadaInterna;
        try {
            firmadaInterna = minioStorageService.generarUrlFirmadaInterna(url, EXPIRY_MINUTOS);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"mensaje\":\"URL de documento inválida.\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"mensaje\":\"No se pudo preparar el documento.\"}");
            return;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(firmadaInterna).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            response.setStatus(status);
            conn.disconnect();
            return;
        }

        String contentType = conn.getContentType();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
        if (contentType != null && !contentType.isBlank()) {
            response.setContentType(contentType);
        }
        long contentLength = conn.getContentLengthLong();
        if (contentLength > 0) {
            response.setContentLengthLong(contentLength);
        }

        try (InputStream is = conn.getInputStream()) {
            is.transferTo(response.getOutputStream());
        } finally {
            conn.disconnect();
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/preview-url")
    public ResponseEntity<?> obtenerPreviewUrl(@RequestParam("url") String url,
                                               @RequestParam("titulo") String titulo,
                                               @RequestParam(name = "mime", required = false) String mime) {
        try {
            return ResponseEntity.ok(Map.of(
                    "previewUrl", documentPreviewService.buildViewerUrl(url, titulo, mime)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "URL de documento inválida."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("mensaje", "No se pudo generar la vista previa."));
        }
    }

    /**
     * Descarga segura: solo SUPERADMIN, ADMINISTRADOR y SOCIO.
     * Streama el archivo desde MinIO a través de Spring con Content-Disposition: attachment.
     * Usa la URL interna de MinIO para funcionar correctamente en Docker.
     * Registra auditoría en cada intento (exitoso o fallido).
     */
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMINISTRADOR', 'SOCIO')")
    @GetMapping("/descargar")
    public void descargarDocumento(
            @RequestParam("url") String url,
            @RequestParam(name = "titulo", required = false, defaultValue = "documento") String titulo,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String ipAddress = obtenerIp(request);
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        Long usuarioId = null;
        String usuarioNombre = userDetails.getUsername();
        try {
            var opt = usuarioService.findByCorreo(userDetails.getUsername());
            if (opt.isPresent()) {
                usuarioId = opt.get().getId();
                usuarioNombre = opt.get().getNombreCompleto();
            }
        } catch (Exception ignored) {}

        final String firmadaInterna;
        try {
            firmadaInterna = minioStorageService.generarUrlFirmadaInterna(url, EXPIRY_MINUTOS);
        } catch (IllegalArgumentException e) {
            auditoriaService.registrarAccionFallida(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_DESCARGAR_DOCUMENTO, AuditoriaService.MODULO_ESTUDIOS,
                    "URL inválida en descarga: \"" + titulo + "\"", e.getMessage(), ipAddress, userAgent);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensaje\":\"URL de documento inválida.\"}");
            return;
        } catch (Exception e) {
            auditoriaService.registrarAccionFallida(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_DESCARGAR_DOCUMENTO, AuditoriaService.MODULO_ESTUDIOS,
                    "Error al preparar descarga: \"" + titulo + "\"", e.getMessage(), ipAddress, userAgent);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensaje\":\"No se pudo preparar el archivo.\"}");
            return;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(firmadaInterna).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            auditoriaService.registrarAccionFallida(usuarioId, usuarioNombre,
                    AuditoriaService.ACCION_DESCARGAR_DOCUMENTO, AuditoriaService.MODULO_ESTUDIOS,
                    "MinIO devolvió " + status + " para: \"" + titulo + "\"", null, ipAddress, userAgent);
            conn.disconnect();
            response.setStatus(status);
            return;
        }

        String ext = extractExtension(url);
        String safeFilename = titulo.replaceAll("[/\\\\:*?\"<>|]", "_") + ext;

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + safeFilename + "\"");
        String contentType = conn.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            response.setContentType(contentType);
        }
        long contentLength = conn.getContentLengthLong();
        if (contentLength > 0) {
            response.setContentLengthLong(contentLength);
        }

        // Auditoría antes de streamear (si el stream falla, ya queda registrado el intento)
        auditoriaService.registrarAccion(usuarioId, usuarioNombre,
                AuditoriaService.ACCION_DESCARGAR_DOCUMENTO, AuditoriaService.MODULO_ESTUDIOS,
                "Descarga autorizada: \"" + titulo + "\"",
                null, null, "EXITOSO", null, ipAddress, userAgent);

        try (InputStream is = conn.getInputStream()) {
            is.transferTo(response.getOutputStream());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Genera la URL del visor bajo demanda (token fresco).
     * Requiere autenticación — impide que URLs del visor en el HTML sean reutilizables sin sesión.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/visor")
    public ResponseEntity<?> visorDocumento(
            @RequestParam("url") String url,
            @RequestParam(name = "titulo", required = false, defaultValue = "documento") String titulo,
            @RequestParam(name = "mime", required = false) String mime) {
        try {
            String previewUrl = documentPreviewService.buildViewerUrl(url, titulo, mime);
            if (previewUrl == null || previewUrl.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "No hay archivo disponible para previsualizar."));
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, previewUrl)
                    .build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "URL de documento inválida."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("mensaje", "No se pudo generar la vista previa."));
        }
    }

    private String obtenerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractExtension(String url) {
        try {
            String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            int dot = path.lastIndexOf('.');
            int slash = path.lastIndexOf('/');
            if (dot > slash && dot > 0) return path.substring(dot).toLowerCase();
        } catch (Exception ignored) {}
        return "";
    }
}

package org.reciclame.demo.controller;

import org.reciclame.demo.service.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Proxy sin sesión para que kkfileview (en Docker) descargue archivos de MinIO.
 * kkfileview no puede acceder directamente a localhost:9000 desde dentro del contenedor,
 * por lo que delega la descarga a este endpoint de Spring Boot, que sí tiene acceso a MinIO.
 * El objectName es un identificador aleatorio ({8-hex}_{nombre}) no adivinable.
 */
@RestController
public class KkfileviewProxyController {

    private static final Logger log = LoggerFactory.getLogger(KkfileviewProxyController.class);

    @Value("${minio.bucket}")
    private String bucket;

    private final MinioStorageService minioStorageService;

    public KkfileviewProxyController(MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    @GetMapping("/api/documentos/kk-proxy")
    public void proxy(@RequestParam("objectName") String objectName,
                      HttpServletResponse response) throws IOException {
        // Validate: plain filename only, no path traversal
        if (objectName == null || objectName.isBlank()
                || objectName.contains("/") || objectName.contains("\\")
                || objectName.contains("..") || objectName.contains("\0")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String archivoUrl = minioStorageService.getMinioUrl() + "/" + bucket + "/" + objectName;
        String presignedUrl;
        try {
            presignedUrl = minioStorageService.generarUrlFirmada(archivoUrl, 10);
        } catch (Exception e) {
            log.error("kk-proxy: error generando URL firmada para '{}': {}", objectName, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(presignedUrl).openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            log.warn("kk-proxy: MinIO devolvió {} para '{}'", status, objectName);
            response.setStatus(status);
            conn.disconnect();
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        String contentType = conn.getContentType();
        if (contentType != null) {
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
}

package org.reciclame.demo.controller;

import org.reciclame.demo.service.DocumentPreviewService;
import org.reciclame.demo.service.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
    private final DocumentPreviewService documentPreviewService;
    private final HttpClient httpClient;

    public KkfileviewProxyController(MinioStorageService minioStorageService,
                                     DocumentPreviewService documentPreviewService,
                                     HttpClient httpClient) {
        this.minioStorageService = minioStorageService;
        this.documentPreviewService = documentPreviewService;
        this.httpClient = httpClient;
    }

    @GetMapping("/api/documentos/kk-proxy")
    public void proxy(@RequestParam("objectName") String objectName,
                      @RequestParam("fileName") String fileName,
                      @RequestParam("token") String token,
                      HttpServletResponse response) throws IOException, InterruptedException {
        proxyInternal(objectName, fileName, token, response);
    }

    @GetMapping("/api/documentos/kk-proxy/{objectName}/{fileName}")
    public void proxyConNombre(@PathVariable("objectName") String objectName,
                               @PathVariable("fileName") String fileName,
                               @RequestParam("token") String token,
                               HttpServletResponse response) throws IOException, InterruptedException {
        proxyInternal(objectName, fileName, token, response);
    }

    private void proxyInternal(String objectName,
                               String fileName,
                               String token,
                               HttpServletResponse response) throws IOException, InterruptedException {
        // Validate: plain filename only, no path traversal
        if (objectName == null || objectName.isBlank()
                || objectName.contains("/") || objectName.contains("\\")
                || objectName.contains("..") || objectName.contains("\0")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\")
                || fileName.contains("..") || fileName.contains("\0")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!documentPreviewService.isValidKkProxyToken(token, objectName, fileName)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String archivoUrl = minioStorageService.getMinioUrl() + "/" + bucket + "/" + objectName;
        String presignedUrl;
        try {
            presignedUrl = minioStorageService.generarUrlFirmadaInterna(archivoUrl, 10);
        } catch (Exception e) {
            log.error("kk-proxy: error generando URL firmada para '{}': {}", objectName, e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(presignedUrl))
                .timeout(Duration.ofSeconds(120))
                .GET()
                .build();

        HttpResponse<InputStream> upstream;
        try {
            upstream = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            log.error("kk-proxy: error conectando a MinIO para '{}': {}", objectName, e.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            return;
        }

        int status = upstream.statusCode();
        if (status != HttpServletResponse.SC_OK) {
            log.warn("kk-proxy: MinIO devolvió {} para '{}'", status, objectName);
            response.setStatus(status);
            upstream.body().close();
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        String contentType = upstream.headers().firstValue("content-type").orElse(null);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        long contentLength = upstream.headers().firstValueAsLong("content-length").orElse(-1L);
        if (contentLength > 0) {
            response.setContentLengthLong(contentLength);
        }

        try (InputStream is = upstream.body()) {
            is.transferTo(response.getOutputStream());
        }
    }
}
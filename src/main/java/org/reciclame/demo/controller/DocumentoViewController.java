package org.reciclame.demo.controller;

import org.reciclame.demo.service.MinioStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Proxy seguro para acceder a archivos almacenados en MinIO.
 *
 * Flujo:
 *   1. El usuario autenticado solicita GET /api/documentos/ver?url=<archivoUrl>
 *   2. Spring Security verifica la sesión antes de entrar al método.
 *   3. Se genera una URL presignada válida por EXPIRY_MINUTOS minutos.
 *   4. Se redirige (302) al cliente hacia esa URL firmada.
 *
 * El bucket MinIO NO debe tener política anónima de descarga;
 * las URLs presignadas funcionan porque llevan credenciales HMAC propias.
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoViewController {

    private static final int EXPIRY_MINUTOS = 15;

    private final MinioStorageService minioStorageService;

    public DocumentoViewController(MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
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
            String firmada = minioStorageService.generarUrlFirmada(url, EXPIRY_MINUTOS);
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
}

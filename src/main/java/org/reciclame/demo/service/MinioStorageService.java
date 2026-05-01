package org.reciclame.demo.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.internal-url}")
    private String minioInternalUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Sube el archivo a MinIO y devuelve la URL pública accesible desde el navegador.
     */
    public String subirArchivo(MultipartFile archivo) {
        try {
            String nombreOriginal = archivo.getOriginalFilename() != null
                    ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "documento";
            String objectName = UUID.randomUUID().toString().substring(0, 8) + "_" + nombreOriginal;

            MinioClient client = buildClient();
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(archivo.getInputStream(), archivo.getSize(), -1)
                            .contentType(archivo.getContentType())
                            .build()
            );
            return minioUrl + "/" + bucket + "/" + objectName;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte la URL pública (localhost:9000) en URL interna (minio:9000)
     * para que kkfileview, corriendo dentro de Docker, pueda alcanzar el archivo.
     */
    public String urlParaVisor(String archivoUrl) {
        if (archivoUrl == null) return null;
        return archivoUrl.replace(minioUrl, minioInternalUrl);
    }

    /**
     * Elimina el archivo del bucket MinIO dado su URL pública.
     * Si falla, registra el error pero no lanza excepción (el registro de DB ya fue eliminado).
     */
    public void eliminarArchivo(String archivoUrl) {
        if (archivoUrl == null || archivoUrl.isBlank()) return;
        try {
            // URL formato: http://localhost:9000/reciclame-docs/{objectName}
            String prefix = minioUrl + "/" + bucket + "/";
            if (!archivoUrl.startsWith(prefix)) {
                log.warn("URL de archivo no corresponde al bucket configurado: {}", archivoUrl);
                return;
            }
            String objectName = archivoUrl.substring(prefix.length());
            buildClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("No se pudo eliminar el archivo de MinIO (url={}): {}", archivoUrl, e.getMessage());
        }
    }

    public String getMinioUrl() {
        return minioUrl;
    }

    public String getMinioInternalUrl() {
        return minioInternalUrl;
    }
}
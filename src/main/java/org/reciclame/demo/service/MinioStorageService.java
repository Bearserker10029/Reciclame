package org.reciclame.demo.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    private MinioClient buildClient(String endpoint) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    private MinioClient buildInternalClient() {
        return buildClient(minioInternalUrl);
    }

    /**
     * Sube el archivo a MinIO y devuelve la URL pública accesible desde el navegador.
     * <p>
     * Implementa reintentos automáticos (3 intentos con backoff) porque MinIO puede
     * devolver "Try again" transitorio cuando está bajo presión de I/O o memoria
     * (especialmente común en EC2 con RAM limitada). El cliente MinIO por default
     * NO reintenta, propagando el error al usuario.
     * </p>
     */
    public String subirArchivo(MultipartFile archivo) throws IOException {
        String nombreOriginal = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        String objectName = UUID.randomUUID().toString().substring(0, 8) + "_" + nombreOriginal;

        MinioClient client = buildInternalClient();
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(archivo.getInputStream(), archivo.getSize(), -1)
                .contentType(archivo.getContentType())
                .build();

        Exception lastError = null;
        // 3 intentos con backoff: 0s, 1s, 2s (cubre MinIO drive-offline recovery).
        for (int intento = 1; intento <= 3; intento++) {
            try {
                client.putObject(args);
                if (intento > 1) {
                    log.info("Upload a MinIO exitoso en el intento {} ({})", intento, objectName);
                }
                return buildPublicObjectUrl(objectName);
            } catch (Exception e) {
                lastError = e;
                log.warn("Upload a MinIO falló en intento {}/3 ({}): {}",
                        intento, objectName, e.getMessage());
                if (intento < 3) {
                    try {
                        Thread.sleep(1000L * intento); // 1s, 2s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Re-abrir el InputStream porque el SDK lo consume en cada intento.
                    try {
                        args = PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(archivo.getInputStream(), archivo.getSize(), -1)
                                .contentType(archivo.getContentType())
                                .build();
                    } catch (IOException ioe) {
                        throw new RuntimeException("No se pudo reabrir el stream para reintento: " + ioe.getMessage(), ioe);
                    }
                }
            }
        }
        throw new RuntimeException("Error al subir archivo a MinIO tras 3 intentos: "
                + (lastError != null ? lastError.getMessage() : "unknown"), lastError);
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
            String objectName = extractObjectName(archivoUrl);
            buildInternalClient().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.warn("URL de archivo no corresponde al bucket configurado: {}", archivoUrl);
        } catch (Exception e) {
            log.error("No se pudo eliminar el archivo de MinIO (url={}): {}", archivoUrl, e.getMessage());
        }
    }

    /**
     * Genera una URL presignada válida por {@code minutosExpiry} minutos.
     * Requiere que el bucket NO tenga política anónima de descarga — los clientes
     * deben pasar por este método (vía el endpoint proxy Spring) para acceder.
     *
     * @param archivoUrl   URL pública almacenada en DB (http://localhost:9000/bucket/objectName)
     * @param minutosExpiry minutos de validez (recomendado: 15 para vistas, 60 para visores)
     */
    public String generarUrlFirmada(String archivoUrl, int minutosExpiry) {
        return generarPresigned(minioUrl, archivoUrl, minutosExpiry);
    }

    /**
     * Igual que {@link #generarUrlFirmada} pero usando la URL interna de MinIO (minio:9000).
     * Usar cuando quien consume la URL es un servicio dentro de la red Docker (ej. kkfileview).
     */
    public String generarUrlFirmadaInterna(String archivoUrl, int minutosExpiry) {
        return generarPresigned(minioInternalUrl, archivoUrl, minutosExpiry);
    }

    /**
     * Genera una URL presignada con host público (para el navegador),
     * conectándose a MinIO vía URL interna (para funcionar dentro de Docker).
     */
    public String generarUrlFirmadaPublica(String archivoUrl, int minutosExpiry) {
        String objectName = extractObjectName(archivoUrl);
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(minioInternalUrl)
                    .credentials(accessKey, secretKey)
                    .build();
            String urlInterna = client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(minutosExpiry, TimeUnit.MINUTES)
                            .build()
            );
            return urlInterna.replace(minioInternalUrl, minioUrl);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar URL firmada pública para: " + objectName, e);
        }
    }

    private String generarPresigned(String endpoint, String archivoUrl, int minutosExpiry) {
        String objectName = extractObjectName(archivoUrl);
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(minutosExpiry, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar URL firmada para: " + objectName, e);
        }
    }

    /**
     * Verifica que el objeto exista en el bucket MinIO.
     * Retorna false si la URL no corresponde al bucket, si el objeto no existe, o si MinIO no es accesible.
     */
    public boolean existeArchivo(String archivoUrl) {
        String objectName;
        try {
            objectName = extractObjectName(archivoUrl);
        } catch (IllegalArgumentException e) {
            return false;
        }
        try {
            buildInternalClient().statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            log.debug("Archivo no encontrado en MinIO ({}): {}", objectName, e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el objectName del bucket a partir de cualquier URL de MinIO,
     * independientemente del host (localhost:9000, minio:9000, host.docker.internal:9000, etc.).
     * Busca el marcador "/{bucket}/" en la ruta y toma lo que viene después.
     */
    private String extractObjectName(String archivoUrl) {
        if (archivoUrl == null || archivoUrl.isBlank()) {
            throw new IllegalArgumentException("URL de archivo nula o vacía");
        }
        String marker = "/" + bucket + "/";
        int idx = archivoUrl.indexOf(marker);
        if (idx < 0) {
            throw new IllegalArgumentException("URL no corresponde al bucket configurado: " + archivoUrl);
        }
        String path = archivoUrl.substring(idx + marker.length());
        int q = path.indexOf('?');
        String objectName = q > 0 ? path.substring(0, q) : path;
        if (objectName.isBlank()) {
            throw new IllegalArgumentException("Object name vacío en URL: " + archivoUrl);
        }
        return objectName;
    }

    /** Extrae el objectName de una URL de MinIO (cualquier hostname). */
    public String getObjectNameFromUrl(String archivoUrl) {
        return extractObjectName(archivoUrl);
    }

    public String getMinioUrl() {
        return minioUrl;
    }

    public String getMinioInternalUrl() {
        return minioInternalUrl;
    }

    private String buildPublicObjectUrl(String objectName) {
        String base = minioUrl.endsWith("/") ? minioUrl.substring(0, minioUrl.length() - 1) : minioUrl;
        return base + "/" + bucket + "/" + objectName;
    }
}

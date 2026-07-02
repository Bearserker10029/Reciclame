package org.reciclame.demo.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.reciclame.demo.helper.EstadoValidacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Servicio que gestiona los archivos subidos ANTES de su validación VirusTotal.
 * <p>
 * Los bytes viven temporalmente en el bucket {@code reciclame-staging}. Solo cuando el
 * worker async confirma que el archivo es seguro (o, en fail-open, cuando no se pudo
 * contactar a VT) se promueve (copy + delete) al bucket definitivo {@code reciclame-docs}.
 * </p>
 * <p>
 * Ambos buckets viven en el mismo MinIO compartido, por lo que cualquier instancia de
 * Spring Boot puede operar sobre los mismos staging objects detrás del balanceador.
 * </p>
 *
 * <p><b>Modelo de URL en BD:</b></p>
 * <ul>
 *   <li>Mientras el archivo está pendiente: {@code staging://<objectName>} en {@code archivo_url},
 *       donde objectName = {@code <uuid>_<sanitizedName>}. El marker embebe el nombre
 *       completo para que get/delete no necesiten volver a derivar el filename.</li>
 *   <li>Tras validación (allowed): URL real MinIO docs, ej. {@code http://minio:9000/reciclame-docs/<uuid>_<name>}.</li>
 *   <li>Tras validación MALICIOUS: sigue en staging y la limpieza TTL lo borra del bucket staging.</li>
 * </ul>
 */
@Service
public class UploadStagingService {

    private static final Logger log = LoggerFactory.getLogger(UploadStagingService.class);

    @Value("${minio.internal-url}")
    private String minioInternalUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket.staging:reciclame-staging}")
    private String stagingBucket;

    @Value("${minio.bucket}")
    private String docsBucket;

    @Value("${minio.url}")
    private String minioPublicUrl;

    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(minioInternalUrl)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Sube el MultipartFile al bucket staging bajo el nombre {@code <uuid>_<sanitizedName>}.
     * Retorna el marker {@code staging://<objectName>} para persistir en {@code archivo_url}.
     */
    public String put(String uuid, MultipartFile archivo) throws IOException {
        String sanitizedName = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        String objectName = uuid + "_" + sanitizedName;
        uploadWithRetries(uuid, objectName, () -> PutObjectArgs.builder()
                .bucket(stagingBucket)
                .object(objectName)
                .stream(archivo.getInputStream(), archivo.getSize(), -1)
                .contentType(archivo.getContentType())
                .build());
        return EstadoValidacion.stagingMarker(objectName);
    }

    /**
     * Sube desde un InputStream (usado por tests o cuando el archivo se reconstruyó desde disco).
     */
    public String putFromStream(String uuid, InputStream in, long size, String contentType, String originalFilename) throws IOException {
        String sanitizedName = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        String objectName = uuid + "_" + sanitizedName;
        uploadWithRetries(uuid, objectName, () -> PutObjectArgs.builder()
                .bucket(stagingBucket)
                .object(objectName)
                .stream(in, size, -1)
                .contentType(contentType)
                .build());
        return EstadoValidacion.stagingMarker(objectName);
    }

    private void uploadWithRetries(String uuid, String objectName, PutObjectSupplier supplier) throws IOException {
        Exception lastError = null;
        for (int intento = 1; intento <= 3; intento++) {
            try {
                buildClient().putObject(supplier.build());
                if (intento > 1) {
                    log.info("Upload staging exitoso en intento {} (uuid={})", intento, uuid);
                }
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("Upload staging falló intento {}/3 (uuid={}): {}", intento, uuid, e.getMessage());
                if (intento < 3) {
                    try { Thread.sleep(1000L * intento); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new IOException("Error subiendo archivo a staging MinIO (uuid=" + uuid + ", obj=" + objectName + "): "
                + (lastError != null ? lastError.getMessage() : "unknown"), lastError);
    }

    @FunctionalInterface
    private interface PutObjectSupplier {
        PutObjectArgs build() throws Exception;
    }

    /**
     * Abre el archivo de staging como InputStream para que VirusTotal lo consuma.
     */
    public InputStream open(String stagingObjectName) throws IOException {
        if (stagingObjectName == null || stagingObjectName.isBlank()) {
            throw new IOException("stagingObjectName vacío");
        }
        try {
            return buildClient().getObject(GetObjectArgs.builder()
                    .bucket(stagingBucket)
                    .object(stagingObjectName)
                    .build());
        } catch (Exception e) {
            throw new IOException("No se pudo abrir staging object " + stagingObjectName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Elimina el archivo del bucket staging. Idempotente: si no existe, no falla.
     */
    public void deleteByObjectName(String stagingObjectName) {
        if (stagingObjectName == null || stagingObjectName.isBlank()) return;
        try {
            buildClient().removeObject(RemoveObjectArgs.builder()
                    .bucket(stagingBucket)
                    .object(stagingObjectName)
                    .build());
            log.debug("Staging eliminado obj={}", stagingObjectName);
        } catch (Exception e) {
            log.warn("No se pudo eliminar staging obj={}: {}", stagingObjectName, e.getMessage());
        }
    }

    /**
     * Promueve el archivo del bucket staging al bucket definitivo docs.
     * <p>
     * MinIO no soporta move atómico entre buckets, por lo que la operación es
     * copy + delete. Si la app muere entre ambos, el job TTL limpia el staging huérfano.
     * Retorna la URL pública definitiva.
     * </p>
     */
    public String promoteToDocs(String stagingObjectName, String contentType) throws IOException {
        if (stagingObjectName == null || stagingObjectName.isBlank()) {
            throw new IOException("stagingObjectName vacío al promover");
        }
        Exception lastError = null;
        for (int intento = 1; intento <= 3; intento++) {
            try {
                buildClient().copyObject(CopyObjectArgs.builder()
                        .bucket(docsBucket)
                        .object(stagingObjectName)
                        .source(CopySource.builder()
                                .bucket(stagingBucket)
                                .object(stagingObjectName)
                                .build())
                        .build());
                try {
                    buildClient().removeObject(RemoveObjectArgs.builder()
                            .bucket(stagingBucket)
                            .object(stagingObjectName)
                            .build());
                } catch (Exception ex) {
                    log.warn("Promoción OK pero no se pudo limpiar staging {}: {}", stagingObjectName, ex.getMessage());
                }
                if (intento > 1) {
                    log.info("Promoción a docs exitosa en intento {} (obj={})", intento, stagingObjectName);
                }
                return buildPublicDocsUrl(stagingObjectName);
            } catch (Exception e) {
                lastError = e;
                log.warn("Promoción a docs falló intento {}/3 (obj={}): {}", intento, stagingObjectName, e.getMessage());
                if (intento < 3) {
                    try { Thread.sleep(1000L * intento); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new IOException("No se pudo promover staging a docs tras 3 intentos (obj=" + stagingObjectName + "): "
                + (lastError != null ? lastError.getMessage() : "unknown"), lastError);
    }

    private String buildPublicDocsUrl(String objectName) {
        String base = minioPublicUrl.endsWith("/")
                ? minioPublicUrl.substring(0, minioPublicUrl.length() - 1)
                : minioPublicUrl;
        return base + "/" + docsBucket + "/" + objectName;
    }

    public String getStagingBucket() {
        return stagingBucket;
    }

    public String getDocsBucket() {
        return docsBucket;
    }
}

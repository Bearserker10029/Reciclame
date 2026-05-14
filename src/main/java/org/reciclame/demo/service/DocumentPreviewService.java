package org.reciclame.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class DocumentPreviewService {

    private static final long KK_PROXY_TOKEN_TTL_SECONDS = 120;

    private final MinioStorageService minioStorageService;
    private final String kkfileviewUrl;
    private final String kkAccessibleUrl;
    private final byte[] signingKey;

    public DocumentPreviewService(MinioStorageService minioStorageService,
                                  @Value("${kkfileview.url}") String kkfileviewUrl,
                                  @Value("${app.kk-accessible-url}") String kkAccessibleUrl,
                                  @Value("${app.document-access-secret:}") String configuredSecret,
                                  @Value("${minio.secret-key}") String minioSecretKey) {
        this.minioStorageService = minioStorageService;
        this.kkfileviewUrl = kkfileviewUrl;
        this.kkAccessibleUrl = kkAccessibleUrl;
        String effectiveSecret = (configuredSecret != null && !configuredSecret.isBlank())
                ? configuredSecret
                : "doc-preview::" + minioSecretKey;
        this.signingKey = effectiveSecret.getBytes(StandardCharsets.UTF_8);
    }

    public String buildViewerUrl(String archivoUrl, String titulo, String archivoTipoMime) {
        if (archivoUrl == null || archivoUrl.isBlank()) return "";
        if (isPdf(archivoUrl, archivoTipoMime)) {
            return "/api/documentos/inline?url=" + URLEncoder.encode(archivoUrl, StandardCharsets.UTF_8);
        }
        return buildKkPreviewUrl(archivoUrl, titulo);
    }

    public String buildKkPreviewUrl(String archivoUrl, String titulo) {
        String objectName = minioStorageService.getObjectNameFromUrl(archivoUrl);
        String fileName = buildFileName(titulo, archivoUrl);
        String token = createKkProxyToken(objectName, fileName);
        String proxyUrl = kkAccessibleUrl + "/api/documentos/kk-proxy/"
                + URLEncoder.encode(objectName, StandardCharsets.UTF_8) + "/"
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String encoded = URLEncoder.encode(
                Base64.getEncoder().encodeToString(proxyUrl.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
        );
        // Usa el proxy reverso Spring en lugar de la URL directa de kkfileview.
        // El browser nunca ve el host/puerto de kkfileview.
        return "/api/documentos/kk-view/onlinePreview?url=" + encoded + "&fullfilename="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }

    public String buildFileName(String titulo, String archivoUrl) {
        return sanitizeFileName(titulo) + extractExtension(archivoUrl);
    }

    public boolean isValidKkProxyToken(String token, String objectName, String fileName) {
        if (token == null || token.isBlank() || objectName == null || fileName == null) return false;
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return false;
        String payloadB64 = token.substring(0, dot);
        String signatureB64 = token.substring(dot + 1);
        byte[] payloadBytes;
        byte[] providedSig;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            providedSig = Base64.getUrlDecoder().decode(signatureB64);
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] expectedSig = hmac(payloadBytes);
        if (!MessageDigest.isEqual(providedSig, expectedSig)) {
            return false;
        }

        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] parts = payload.split("\n", 3);
        if (parts.length != 3) return false;
        if (!objectName.equals(parts[0]) || !fileName.equals(parts[1])) return false;

        long expiresAt;
        try {
            expiresAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        return Instant.now().getEpochSecond() <= expiresAt;
    }

    private String createKkProxyToken(String objectName, String fileName) {
        long expiresAt = Instant.now().plusSeconds(KK_PROXY_TOKEN_TTL_SECONDS).getEpochSecond();
        byte[] payloadBytes = (objectName + "\n" + fileName + "\n" + expiresAt).getBytes(StandardCharsets.UTF_8);
        byte[] signature = hmac(payloadBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private byte[] hmac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token de vista previa.", e);
        }
    }

    private boolean isPdf(String archivoUrl, String archivoTipoMime) {
        if (archivoTipoMime != null && archivoTipoMime.toLowerCase().contains("pdf")) {
            return true;
        }
        return archivoUrl != null && archivoUrl.toLowerCase().contains(".pdf");
    }

    private String sanitizeFileName(String titulo) {
        if (titulo == null || titulo.isBlank()) return "documento";
        return titulo.trim()
                .replaceAll("[/\\\\:*?\"<>|]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_");
    }

    private String extractExtension(String archivoUrl) {
        try {
            String path = archivoUrl.contains("?") ? archivoUrl.substring(0, archivoUrl.indexOf('?')) : archivoUrl;
            int dot = path.lastIndexOf('.');
            int slash = path.lastIndexOf('/');
            if (dot > slash && dot > 0) {
                return path.substring(dot).toLowerCase();
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}

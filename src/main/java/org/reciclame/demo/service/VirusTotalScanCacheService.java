package org.reciclame.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reciclame.demo.dto.VirusTotalVerdict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Caché de veredictos de VirusTotal en Redis.
 *
 * <p>Clave: {@code vt:sha256:<sha256>} → JSON con el {@link VirusTotalVerdict}.</p>
 * <p>TTL por defecto: 24 h (configurable).</p>
 *
 * <p>Si Redis está caído o no accesible, el método devuelve {@link Optional#empty()}
 * silenciosamente: preferimos pedirle a VT de nuevo antes que romper la subida del
 * archivo. El cacheo es una optimización, no una fuente de verdad.</p>
 */
@Service
public class VirusTotalScanCacheService {

    private static final Logger log = LoggerFactory.getLogger(VirusTotalScanCacheService.class);
    private static final String KEY_PREFIX = "vt:sha256:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public VirusTotalScanCacheService(StringRedisTemplate redis,
                                     @Value("${virustotal.cache.ttl-hours:24}") int ttlHours) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // para java.time.Instant
        this.ttl = Duration.ofHours(ttlHours);
    }

    public Optional<VirusTotalVerdict> get(String sha256) {
        if (sha256 == null) return Optional.empty();
        try {
            String json = redis.opsForValue().get(KEY_PREFIX + sha256);
            if (json == null) return Optional.empty();
            VirusTotalVerdict v = objectMapper.readValue(json, VirusTotalVerdict.class);
            return Optional.of(v);
        } catch (Exception e) {
            log.warn("VirusTotal cache lookup falló (sha={}): {}", sha256, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(VirusTotalVerdict verdict) {
        if (verdict == null || verdict.getSha256() == null) return;
        try {
            String json = objectMapper.writeValueAsString(verdict);
            redis.opsForValue().set(KEY_PREFIX + verdict.getSha256(), json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("VirusTotal cache store falló (sha={}): {}", verdict.getSha256(), e.getMessage());
        } catch (Exception e) {
            log.warn("VirusTotal cache Redis error (sha={}): {}", verdict.getSha256(), e.getMessage());
        }
    }
}
package org.reciclame.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Tokens de inicio de sesión por QR.
 *
 * <p>Flujo:</p>
 * <ol>
 *   <li>Usuario logueado abre su perfil → backend genera token y lo guarda en Redis
 *       con TTL 5 min (clave: {@code qrlogin:<token>} → valor: {@code userId}).</li>
 *   <li>El QR contiene una URL pública {@code /auth/qr?token=<token>}.</li>
 *   <li>Al escanear el QR desde el celular, el navegador visita esa URL; el backend
 *       consume el token (borrado atómico) y crea la sesión del usuario.</li>
 * </ol>
 *
 * <p>El token es de un solo uso: una vez consumido, ya no sirve. Esto evita que
 * alguien que capture el QR pueda reutilizarlo.</p>
 */
@Service
public class QrLoginService {

    private static final Logger log = LoggerFactory.getLogger(QrLoginService.class);
    private static final String KEY_PREFIX = "qrlogin:";
    private static final SecureRandom RNG = new SecureRandom();

    private final StringRedisTemplate redis;
    private final ParametrosSistemaService parametrosService;

    public QrLoginService(StringRedisTemplate redis, ParametrosSistemaService parametrosService) {
        this.redis = redis;
        this.parametrosService = parametrosService;
    }

    /**
     * Genera un nuevo token QR (magic link) para el usuario indicado.
     * El TTL es parametrizable (parametros_sistema.magic_link_expiracion_min)
     * y el token es de un solo uso.
     */
    public String generarToken(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuarioId no puede ser null");
        }
        int minutosExpiracion = parametrosService.getIntegerOrDefault("magic_link_expiracion_min", 10);
        Duration ttl = Duration.ofMinutes(minutosExpiracion);
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            redis.opsForValue().set(KEY_PREFIX + token, String.valueOf(usuarioId), ttl);
        } catch (Exception e) {
            log.error("No se pudo guardar token QR en Redis: {}", e.getMessage());
            throw new IllegalStateException("Servicio de QR no disponible", e);
        }
        return token;
    }

    /**
     * Minutos de vida configurados actualmente para el token (uso informativo, p.ej. UI).
     */
    public int obtenerMinutosExpiracion() {
        return parametrosService.getIntegerOrDefault("magic_link_expiracion_min", 10);
    }

    /**
     * Consume un token: lo borra de Redis y devuelve el usuarioId si era válido.
     * Operación atómica: el token desaparece aunque haya carrera entre dos requests.
     */
    public Optional<Long> consumirToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            String key = KEY_PREFIX + token;
            String valor = redis.opsForValue().getAndDelete(key);
            if (valor == null) return Optional.empty();
            return Optional.of(Long.parseLong(valor));
        } catch (NumberFormatException e) {
            log.warn("Token QR con valor no numérico: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error consumiendo token QR: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

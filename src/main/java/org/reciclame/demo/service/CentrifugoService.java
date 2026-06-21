package org.reciclame.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CentrifugoService {

    private static final Logger log = LoggerFactory.getLogger(CentrifugoService.class);

    private final String apiUrl;
    private final String apiKey;
    private final SecretKeySpec hmacKey;
    private final RestClient http = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public CentrifugoService(
            @Value("${centrifugo.api.url}") String apiUrl,
            @Value("${centrifugo.api.key}") String apiKey,
            @Value("${centrifugo.secret}") String secret) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.hmacKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** JWT que el frontend usa para conectarse al WebSocket de Centrifugo. */
    public String generarTokenConexion(Long usuarioId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 24L * 60 * 60 * 1000))
                .signWith(hmacKey, Jwts.SIG.HS256)
                .compact();
    }

    /** Publica un mensaje en un canal de Centrifugo (todos los suscritos lo reciben). */
    public void publicar(String canal, Object payload) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("channel", canal);
            body.put("data", payload);

            http.post()
                    .uri(apiUrl + "/publish")
                    .header(HttpHeaders.AUTHORIZATION, "apikey " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("[Centrifugo] Error publicando en {}: {}", canal, ex.getMessage());
        }
    }
}

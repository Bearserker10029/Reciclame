package org.reciclame.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP saliente basado en java.net.http.HttpClient (JDK 26).
 *
 * Preferimos HTTP/3 (QUIC sobre UDP) cuando el servidor destino lo soporta.
 * Si no hay respuesta QUIC o falla la conexión, el cliente baja
 * automáticamente a HTTP/2 y luego a HTTP/1.1, así que es seguro activarlo
 * sin coordinación con MinIO / kkfileview / otros servicios internos.
 *
 * JEP 517: https://openjdk.org/jeps/517
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient reciclameHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
}
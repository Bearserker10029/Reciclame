package org.reciclame.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Cliente HTTP saliente basado en java.net.http.HttpClient (JDK 26).
 *
 * Preferimos HTTP/3 (QUIC sobre UDP) cuando el servidor destino lo soporta.
 * Si no hay respuesta QUIC o falla la conexión, el cliente baja
 * automáticamente a HTTP/2 y luego a HTTP/1.1, así que es seguro activarlo
 * sin coordinación con MinIO / kkfileview / otros servicios internos.
 *
 * <p><b>Optimización para descargas de archivos pesados:</b></p>
 * <ul>
 *   <li>Pool de threads dedicado (cached) con keep-alive corto para que muchas
 *       descargas simultáneas (visores Office, kkfileview) no se bloqueen entre sí.</li>
 *   <li>Connect timeout corto (10 s) para fallar rápido y reintentar antes.</li>
 * </ul>
 *
 * JEP 517: https://openjdk.org/jeps/517
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient reciclameHttpClient() {
        // Cached thread pool: crea threads según demanda, reusa los idle hasta 30 s.
        // Esto evita el cuello de botella cuando múltiples usuarios abren visores en paralelo.
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "reciclame-http");
            t.setDaemon(true);
            return t;
        });
        executor.setKeepAliveTime(30, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);

        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_3)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .executor(executor)
                .build();
    }
}

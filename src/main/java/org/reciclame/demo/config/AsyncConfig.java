package org.reciclame.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool de threads dedicado al scanner async de uploads que valida contra VirusTotal
 * antes de promover los archivos del bucket staging al bucket definitivo.
 *
 * <p>Tuning para t3.large con 2 instancias Spring (4 hilos totales de procesamiento VT
 * posibles): respeta el límite público de VirusTotal (4 req/min) y deja margen en RAM.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${upload.scanner.pool.core-size:1}")
    private int coreSize;

    @Value("${upload.scanner.pool.max-size:2}")
    private int maxSize;

    @Value("${upload.scanner.pool.queue-capacity:50}")
    private int queueCapacity;

    @Bean(name = "stagingScannerExecutor")
    public Executor stagingScannerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("staging-scanner-");
        // El scanner hace I/O bloqueante (HTTP a VT, MinIO streaming): ignorar en caso de saturación.
        executor.setRejectedExecutionHandler((r, exec) -> {
            log.warn("stagingScannerExecutor saturado, descartando tarea (queue={}, active={}, pool={})",
                    exec.getQueue().size(), exec.getActiveCount(), exec.getPoolSize());
            throw new java.util.concurrent.RejectedExecutionException(
                    "Scanner pool saturado, se descarta la validación (reintentable vía @Scheduled)");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("stagingScannerExecutor inicializado: core={}, max={}, queue={}", coreSize, maxSize, queueCapacity);
        return executor;
    }
}

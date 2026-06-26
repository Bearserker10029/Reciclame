package org.reciclame.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.serializer.StandardJavaScriptSerializer;

/**
 * Reemplaza el {@code JavaScriptSerializer} por defecto de Thymeleaf por uno que
 * sabe serializar {@link java.time.LocalDate}, {@link java.time.Instant} y demás
 * tipos de {@code java.time}.
 *
 * <p>Sin esto, cualquier template con {@code <script th:inline="javascript">} que
 * use {@code /*[[${objeto}]]*\/} sobre un DTO con campos de fecha/hora lanza:</p>
 *
 * <pre>Java 8 date/time type java.time.LocalDate not supported by default</pre>
 *
 * <p>El error rompe el render de TODA la página: ningún botón ni handler JS
 * funciona porque el script entero queda mal formado.</p>
 */
@Configuration
public class ThymeleafJavaScriptSerializerConfig {

    private static final Logger log = LoggerFactory.getLogger(ThymeleafJavaScriptSerializerConfig.class);

    private final SpringTemplateEngine templateEngine;

    public ThymeleafJavaScriptSerializerConfig(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @PostConstruct
    void registrarSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // Fechas como strings ISO-8601 (no como timestamps), formato amigable para JS.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        StandardDialect standard = templateEngine.getDialects().stream()
                .filter(StandardDialect.class::isInstance)
                .map(StandardDialect.class::cast)
                .findFirst()
                .orElse(null);

        if (standard == null) {
            log.warn("No se encontró StandardDialect en Thymeleaf; el inline JS seguirá con el serializer por defecto.");
            return;
        }

        standard.setJavaScriptSerializer(new StandardJavaScriptSerializer(mapper));
        log.info("Thymeleaf JavaScript serializer configurado con JavaTimeModule.");
    }
}

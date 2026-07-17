package org.reciclame.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

/**
 * Habilita explícitamente las sesiones vía Spring Session JDBC en el perfil
 * local. Con spring-session-jdbc y spring-session-data-redis en el classpath
 * a la vez (ver pom.xml), Spring Boot no auto-configura ninguna de las dos
 * sin esta anotación explícita — de ahí que {@link RedisSessionConfig} exista
 * para el perfil aws. Sin este equivalente para local, no hay ningún bean
 * {@code FindByIndexNameSessionRepository} disponible (necesario para poder
 * invalidar la sesión de un usuario cuando le cambian el rol, lo bloquean o
 * lo desactivan — ver UsuarioService).
 */
@Configuration
@Profile("local")
@EnableJdbcHttpSession
public class JdbcSessionConfig {
}

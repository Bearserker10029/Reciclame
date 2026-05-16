package org.reciclame.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final AuthenticationSuccessHandler successHandler;
    private final AccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          AuthenticationSuccessHandler successHandler,
                          AccessDeniedHandler accessDeniedHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/logout", "/registro", "/registro/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/legal/**").permitAll()
                        .requestMatchers("/css/**", "/style.css", "/assets/**", "/js/**",
                                "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        // Descarga segura: solo roles con permiso de descarga
                        .requestMatchers("/api/documentos/descargar").hasAnyRole("SUPERADMIN", "ADMINISTRADOR", "SOCIO")
                        // Proxy reverso de kkfileview: requiere sesión (el browser nunca toca kkfileview directamente)
                        .requestMatchers("/api/documentos/kk-view/**").authenticated()
                        // Visor autenticado: cualquier usuario con sesión (token fresco en cada carga)
                        .requestMatchers("/api/documentos/visor").authenticated()
                        // Proxy seguro de documentos MinIO: cualquier usuario autenticado
                        .requestMatchers("/api/documentos/ver").authenticated()
                        // Proxy interno para kkfileview (sin sesión, objectName no adivinable)
                        .requestMatchers("/api/documentos/kk-proxy", "/api/documentos/kk-proxy/**").permitAll()
                        // API de usuarios: solo ADMINISTRADOR y SUPERADMIN
                        .requestMatchers("/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "SUPERADMIN")
                        // Impersonación: solo SUPERADMIN o IMPERSONATOR
                        .requestMatchers("/superadmin/impersonar/**").hasAnyRole("SUPERADMIN", "IMPERSONATOR")
                        // Rutas de vistas por rol
                        .requestMatchers("/admin/**").hasAnyRole("ADMINISTRADOR", "SUPERADMIN")
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/socio/**").hasAnyRole("SOCIO", "IMPERSONATOR")
                        .requestMatchers("/visualizador/**").hasAnyRole("VISUALIZADOR", "IMPERSONATOR")
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        // Support both container sessions (dev) and Spring Session cookie (prod)
                        .deleteCookies("JSESSIONID", "SESSION")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler)
                        // Solicitudes no autenticadas a endpoints de documentos → 401 en vez de redirect al login.
                        // Incluye kk-view/** para que las imágenes de diapositivas (cargadas por <img> tags)
                        // reciban 401 y no una redirección HTML al login que el browser no puede renderizar.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                req -> req.getRequestURI().startsWith("/api/documentos/")
                        )
                )
                .headers(headers -> headers
                        // Permite embeber rutas same-origin como /api/documentos/inline dentro del visor.
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

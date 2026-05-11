package org.reciclame.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final AuthenticationSuccessHandler successHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
                          AuthenticationSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/logout", "/registro", "/registro/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/css/**", "/style.css", "/assets/**", "/js/**",
                                "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        // Proxy seguro de documentos MinIO: cualquier usuario autenticado
                        .requestMatchers("/api/documentos/ver").authenticated()
                        // Proxy interno para kkfileview (sin sesión, objectName no adivinable)
                        .requestMatchers("/api/documentos/kk-proxy").permitAll()
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
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
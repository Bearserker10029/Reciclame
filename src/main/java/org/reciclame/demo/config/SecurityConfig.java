package org.reciclame.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
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
                        .requestMatchers("/css/**", "/style.css", "/assets/**", "/js/**",
                                "/images/**", "/fonts/**", "/favicon.ico").permitAll()
                        .requestMatchers("/superadmin/impersonar/**").hasAnyRole("SUPERADMIN", "IMPERSONATOR")
                        .requestMatchers("/admin/**").hasAnyRole("ADMINISTRADOR", "SUPERADMIN")
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/socio/**").hasRole("SOCIO")
                        .requestMatchers("/visualizador/**").hasRole("VISUALIZADOR")
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
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
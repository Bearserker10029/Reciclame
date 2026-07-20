package org.reciclame.demo.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.util.HttpClientIpResolver;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

/**
 * Maneja los intentos de login fallidos:
 *  1. Incrementa el contador {@code intentos_fallidos} del usuario.
 *  2. Si el contador alcanza el valor parametrizado en
 *     {@code parametros_sistema.max_intentos_login}, marca la cuenta como
 *     bloqueada (cuenta_bloqueada=true, estado_cuenta='BLOQUEADO'),
 *     resetea el contador y registra la auditoría.
 *  3. Redirige al login canónico con un flag para que la UI muestre un
 *     mensaje específico cuando la cuenta quedó bloqueada.
 */
@Component
public class AuthFailureHandler implements AuthenticationFailureHandler {

    private final UsuarioRepository usuarioRepository;
    private final ParametrosSistemaService parametrosService;
    private final AuditoriaService auditoriaService;

    public AuthFailureHandler(UsuarioRepository usuarioRepository,
                              ParametrosSistemaService parametrosService,
                              AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.parametrosService = parametrosService;
        this.auditoriaService = auditoriaService;
    }

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        boolean locked = false;
        if (username != null && !username.isBlank()) {
            String correoNormalizado = username.trim().toLowerCase();
            var maybeUser = usuarioRepository.findByCorreoCorporativo(correoNormalizado);
            if (maybeUser.isPresent()) {
                Usuario u = maybeUser.get();
                usuarioRepository.incrementarIntentosFallidos(u.getId());

                int intentos = u.getIntentosFallidos() == null ? 0 : u.getIntentosFallidos() + 1;

                // ── Auditoría: registrar el intento fallido individual ──
                // Aparece en la pantalla de Auditoría porque la query
                // findEventosSeguridad incluye estado='FALLIDO'. Módulo
                // AUTENTICACION (no SEGURIDAD) para que sea consistente
                // con el LOGIN exitoso del LoginController.
                try {
                    auditoriaService.registrarAccion(
                            u.getId(),
                            u.getNombreCompleto(),
                            AuditoriaService.ACCION_LOGIN,
                            "AUTENTICACION",
                            "Intento de login fallido (" + intentos + "º intento) — " +
                                    exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                            null, null, "FALLIDO",
                            exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                            ip, userAgent);
                } catch (Exception ignore) { /* auditoría no debe tumbar el login */ }

                int maxIntentos = parametrosService.getIntegerOrDefault("max_intentos_login", 5);
                if (intentos >= maxIntentos) {
                    usuarioRepository.bloquearCuenta(u.getId());
                    locked = true;
                    try {
                        auditoriaService.registrarAccion(
                                u.getId(),
                                u.getNombreCompleto(),
                                "BLOQUEO_AUTOMATICO_LOGIN",
                                AuditoriaService.MODULO_SEGURIDAD,
                                "Cuenta bloqueada automáticamente tras " + intentos +
                                        " intentos fallidos de login (max_intentos_login=" + maxIntentos + ").",
                                null, null, "EXITOSO", null, ip, userAgent);
                    } catch (Exception ignore) { /* auditoría no debe tumbar el login */ }
                }
            } else {
                // Usuario inexistente: registrar como FALLIDO sin poder
                // asociarlo a un id (el panel mostrará "Sistema").
                try {
                    auditoriaService.registrarAccion(
                            null,
                            correoNormalizado,
                            AuditoriaService.ACCION_LOGIN,
                            "AUTENTICACION",
                            "Intento de login con usuario inexistente: " + correoNormalizado +
                                    " — " + exception.getClass().getSimpleName(),
                            null, null, "FALLIDO",
                            "Usuario no encontrado: " + correoNormalizado,
                            ip, userAgent);
                } catch (Exception ignore) { /* auditoría no debe tumbar el login */ }
            }
        }

        StringBuilder url = new StringBuilder("/service/repositorio/login?error=true");
        if (locked) url.append("&locked=true");
        response.sendRedirect(url.toString());
    }
}
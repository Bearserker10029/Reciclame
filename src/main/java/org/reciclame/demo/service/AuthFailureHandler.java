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
        boolean locked = false;
        if (username != null && !username.isBlank()) {
            var maybeUser = usuarioRepository.findByCorreoCorporativo(username.trim().toLowerCase());
            if (maybeUser.isPresent()) {
                Usuario u = maybeUser.get();
                usuarioRepository.incrementarIntentosFallidos(u.getId());

                int maxIntentos = parametrosService.getIntegerOrDefault("max_intentos_login", 5);
                int intentos = u.getIntentosFallidos() == null ? 0 : u.getIntentosFallidos() + 1;
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
                                null, null, "EXITOSO", null,
                                HttpClientIpResolver.resolve(request), request.getHeader("User-Agent"));
                    } catch (Exception ignore) { /* auditoría no debe tumbar el login */ }
                }
            }
        }

        StringBuilder url = new StringBuilder("/service/repositorio/login?error=true");
        if (locked) url.append("&locked=true");
        response.sendRedirect(url.toString());
    }
}
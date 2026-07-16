package org.reciclame.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloquea toda la app hasta que un usuario con contraseña temporal
 * (debeCambiarPassword=true en su sesión) defina su propia contraseña.
 *
 * Se apoya en el flag ya cacheado en {@link UsuarioSesion} (seteado en el
 * login) para no pegarle a la BD en cada request. Cuando el usuario cambia
 * la contraseña, {@code PasswordResetController} pone ese flag en false en
 * la misma sesión, así que deja de bloquear sin necesidad de re-loguearse.
 */
@Component
public class ForzarCambioPasswordFilter extends OncePerRequestFilter {

    private static final String[] RUTAS_PERMITIDAS = {
            "/auth/", "/logout", "/css/", "/style.css", "/assets/", "/js/",
            "/images/", "/fonts/", "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute("usuarioSesion");
            if (attr instanceof UsuarioSesion us && us.isDebeCambiarPassword() && !esRutaPermitida(request)) {
                response.sendRedirect(request.getContextPath() + "/auth/cambiar-password-inicial");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean esRutaPermitida(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String base = request.getContextPath();
        for (String prefijo : RUTAS_PERMITIDAS) {
            if (uri.startsWith(base + prefijo)) {
                return true;
            }
        }
        return false;
    }
}

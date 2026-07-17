package org.reciclame.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.service.MantenimientoGlobalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bloquea toda la app para todos los roles excepto SUPERADMIN mientras
 * {@link MantenimientoGlobalService} está activo (p. ej. durante una
 * restauración de backup, que manipula las tablas en vivo).
 */
@Component
public class MantenimientoGlobalFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoGlobalFilter.class);

    private static final String[] RUTAS_PERMITIDAS = {
            "/auth/", "/logout", "/mantenimiento", "/css/", "/style.css", "/assets/",
            "/js/", "/images/", "/fonts/", "/favicon.ico"
    };

    private final MantenimientoGlobalService mantenimientoService;

    public MantenimientoGlobalFilter(MantenimientoGlobalService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!mantenimientoService.estaActivo() || esRutaPermitida(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute("usuarioSesion");
            if (attr instanceof UsuarioSesion us && "SUPERADMIN".equals(us.getRolNombre())) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String uri = request.getRequestURI();
        if (uri.startsWith(request.getContextPath() + "/api/")) {
            log.info("Mantenimiento activo: bloqueando API {} ({})", uri, mantenimientoService.getMotivo());
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensaje\":\"Sistema en mantenimiento. Intenta de nuevo en unos minutos.\",\"tipo\":\"MANTENIMIENTO\"}");
            return;
        }

        log.info("Mantenimiento activo: redirigiendo {} a /mantenimiento ({})", uri, mantenimientoService.getMotivo());
        response.sendRedirect(request.getContextPath() + "/mantenimiento");
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

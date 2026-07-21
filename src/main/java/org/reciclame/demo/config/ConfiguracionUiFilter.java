package org.reciclame.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enforce server-side de la configuración de UI por rol.
 * <p>
 * El {@code ConfiguracionUiRolService} controla qué módulos se muestran en el
 * sidebar de cada rol (vía {@code configuracion-vistas.html}). Antes de este
 * filtro, esa configuración era solo decorativa del lado cliente: si el admin
 * desactivaba "normativo" en la pantalla de configuración, el sidebar lo
 * ocultaba, pero el admin podía entrar igual tipeando la URL
 * {@code /admin/normativo/3/ver} en el browser o haciendo click en un
 * resultado de la búsqueda global del topbar.
 * </p>
 * <p>
 * Este filtro cierra ese hueco: extrae el módulo de la URL, lo busca en la
 * lista permitida para el rol del usuario, y si no está, redirige a la página
 * de /mantenimiento (browser) o devuelve 403 JSON (fetch/JS).
 * </p>
 * <p>
 * <strong>Bypass para SUPERADMIN:</strong> el superadmin siempre pasa, sin
 * importar la configuración, para que pueda entrar a un módulo que
 * accidentalmente desactivó para sí mismo y reactivarlo.
 * </p>
 */
@Component
public class ConfiguracionUiFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionUiFilter.class);

    /**
     * Módulos que siempre son accesibles para cualquier rol autenticado, porque
     * son transversales (dashboard principal, perfil propio, logout, errores).
     * El filtro los ignora aunque no estén en la lista del rol.
     */
    private static final Set<String> MODULOS_SIEMPRE_PERMITIDOS = Set.of(
            "inicio",
            "perfil",
            "cambiar-password"
    );

    private static final Map<String, String> MODULE_ALIASES = Map.of(
            "documentos", "admin-docs"
    );

    private static final List<String> ROL_PREFIXES = Arrays.asList(
            "/admin", "/superadmin", "/socio", "/visualizador"
    );

    private final ConfiguracionUiRolService service;

    public ConfiguracionUiFilter(ConfiguracionUiRolService service) {
        this.service = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String prefix = extractRolPrefix(uri);
        if (prefix == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String modulo = extractModulo(uri, prefix);
        if (modulo == null) {
            filterChain.doFilter(request, response);
            return;
        }
        modulo = normalizeModule(modulo);

        if (MODULOS_SIEMPRE_PERMITIDOS.contains(modulo)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            filterChain.doFilter(request, response);
            return;
        }
        Object attr = session.getAttribute("usuarioSesion");
        if (!(attr instanceof UsuarioSesion usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rol = usuario.getRolNombre();
        if (rol == null || "SUPERADMIN".equals(rol)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> modulosPermitidos = service.getModulosSidebar(rol);
        if (modulosPermitidos == null || !modulosPermitidos.contains(modulo)) {
            log.warn("Módulo deshabilitado para el rol: usuario={} rol={} modulo={} uri={}",
                    usuario.getCorreoCorporativo(), rol, modulo, uri);

            // Mismo criterio que el resto de filtros de la app: si el cliente
            // espera HTML (browser navegando) lo mandamos a la página diseñada
            // de /mantenimiento. Si espera JSON (fetch/JS) le devolvemos 403
            // con body estructurado para que el frontend lo maneje.
            String accept = request.getHeader("Accept");
            boolean quiereHtml = accept != null
                    && (accept.contains("text/html") || accept.contains("application/xhtml+xml"));

            if (quiereHtml) {
                log.info("Redirigiendo {} a /mantenimiento (módulo '{}' deshabilitado para rol {})",
                        uri, modulo, rol);
                response.sendRedirect(request.getContextPath() + "/mantenimiento");
                return;
            }

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            String body = "{\"mensaje\":\"No tienes permiso para acceder al módulo '" + modulo
                    + "'. Fue deshabilitado por el SuperAdmin en Configuración > Vistas por rol.\","
                    + "\"tipo\":\"MODULO_DESHABILITADO\","
                    + "\"modulo\":\"" + modulo + "\","
                    + "\"rol\":\"" + rol + "\"}";
            response.getWriter().write(body);
            response.getWriter().flush();
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractRolPrefix(String uri) {
        for (String prefix : ROL_PREFIXES) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return prefix;
            }
        }
        return null;
    }

    /**
     * Devuelve el módulo de la URL:
     * {@code /admin/normativo/3/ver} → "normativo"
     * {@code /superadmin/auditoria}   → "auditoria"
     * {@code /admin}                  → null (es solo el prefijo de rol)
     * {@code /admin/}                 → null
     */
    private String normalizeModule(String modulo) {
        return MODULE_ALIASES.getOrDefault(modulo, modulo);
    }

    private String extractModulo(String uri, String prefix) {
        if (!uri.startsWith(prefix + "/")) {
            return null;
        }
        String resto = uri.substring(prefix.length() + 1);
        int slash = resto.indexOf('/');
        String mod = (slash < 0) ? resto : resto.substring(0, slash);
        return mod.isBlank() ? null : mod;
    }
}

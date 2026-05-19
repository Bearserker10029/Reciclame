package org.reciclame.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

/**
 * Intercepta requests de SOCIO y VISUALIZADOR y bloquea el acceso
 * a módulos que hayan sido desactivados en Configurar Vistas.
 */
@Component
public class EndpointAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EndpointAccessInterceptor.class);

    private final ConfiguracionUiRolService uiConfigService;

    public EndpointAccessInterceptor(ConfiguracionUiRolService uiConfigService) {
        this.uiConfigService = uiConfigService;
    }

    /** Mapeo de módulo → patrones de URL para SOCIO */
    private static final Map<String, List<String>> SOCIO_MODULE_PATTERNS = Map.of(
            "inicio",          List.of("/socio", "/socio/inicio"),
            "estudios",        List.of("/socio/estudios"),
            "normativo",       List.of("/socio/normativo"),
            "mi-unidad",       List.of("/socio/mi-unidad"),
            "reportes",        List.of("/socio/reportes"),
            "perfil",          List.of("/socio/perfil"),
            "notificaciones",  List.of("/socio/notificaciones")
    );

    /** Mapeo de módulo → patrones de URL para VISUALIZADOR */
    private static final Map<String, List<String>> VISUALIZADOR_MODULE_PATTERNS = Map.of(
            "inicio",          List.of("/visualizador", "/visualizador/inicio"),
            "estudios",        List.of("/visualizador/estudios"),
            "normativo",       List.of("/visualizador/normativo"),
            "perfil",          List.of("/visualizador/perfil"),
            "notificaciones",  List.of("/visualizador/notificaciones")
    );

    /** Mapeo de módulo → patrones de URL para ADMINISTRADOR */
    private static final Map<String, List<String>> ADMIN_MODULE_PATTERNS = Map.of(
            "inicio",          List.of("/admin", "/admin/inicio"),
            "estudios",        List.of("/admin/estudios"),
            "normativo",       List.of("/admin/normativo"),
            "usuarios",        List.of("/admin/usuarios"),
            "admin-docs",      List.of("/admin/admin-docs", "/admin/documentos"),
            "trazabilidad",    List.of("/admin/trazabilidad"),
            "reportes",        List.of("/admin/reportes"),
            "diccionarios",    List.of("/admin/diccionarios"),
            "perfil",          List.of("/admin/perfil"),
            "notificaciones",  List.of("/admin/notificaciones")
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }

        String rol = resolveRol(auth);
        if (rol == null) {
            return true;
        }

        if ("SUPERADMIN".equals(rol)) {
            return true;
        }

        String uri = request.getRequestURI();
        String modulo = findModuleForUri(rol, uri);
        if (modulo == null || "inicio".equals(modulo)) {
            return true;
        }

        List<String> modulosPermitidos = uiConfigService.getModulosSidebar(rol);
        if (modulosPermitidos.contains(modulo)) {
            return true;
        }

        log.warn("Acceso denegado: rol={} intentó acceder a módulo '{}' (URI={}). Redirigiendo a mantenimiento.", rol, modulo, uri);
        try {
            response.sendRedirect("/mantenimiento");
        } catch (java.io.IOException e) {
            log.error("Error al redirigir a mantenimiento", e);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return false;
    }

    private String resolveRol(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            switch (ga.getAuthority()) {
                case "ROLE_SUPERADMIN":     return "SUPERADMIN";
                case "ROLE_ADMINISTRADOR":  return "ADMINISTRADOR";
                case "ROLE_SOCIO":          return "SOCIO";
                case "ROLE_VISUALIZADOR":   return "VISUALIZADOR";
                case "ROLE_IMPERSONATOR":   return "SOCIO";
            }
        }
        return null;
    }

    private String findModuleForUri(String rol, String uri) {
        Map<String, List<String>> patterns;
        if ("VISUALIZADOR".equals(rol)) {
            patterns = VISUALIZADOR_MODULE_PATTERNS;
        } else if ("ADMINISTRADOR".equals(rol)) {
            patterns = ADMIN_MODULE_PATTERNS;
        } else {
            patterns = SOCIO_MODULE_PATTERNS;
        }

        for (Map.Entry<String, List<String>> entry : patterns.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (matchUri(pattern, uri)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Compara un patrón con un URI.
     * Soporta sufijo /** para coincidencia de sub-rutas.
     */
    private boolean matchUri(String pattern, String uri) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return uri.equals(prefix) || uri.startsWith(prefix + "/");
        }
        return uri.equals(pattern);
    }
}

package org.reciclame.demo.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HttpClientIpResolver {

    private HttpClientIpResolver() {
    }

    /**
     * Devuelve la IP del cliente detrás de proxies.
     * Orden: X-Forwarded-For (primer hop no-unknown) → X-Real-IP → remoteAddr.
     * XFF puede venir como cadena CSV "cliente, proxy1, proxy2";
     * se devuelve siempre el primer elemento no vacío/no-unknown.
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            for (String part : xff.split(",")) {
                String ip = part.trim();
                if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip;
                }
            }
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank() && !"unknown".equalsIgnoreCase(xri)) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Variante para usar desde cualquier punto del código que esté en el thread
     * de un request HTTP (controllers, services invocados desde controllers, etc.).
     * Lee el request actual del {@link RequestContextHolder} que Spring mantiene
     * por thread durante el ciclo de vida del request.
     * <p>
     * Devuelve {@code null} si no hay request activo (ej. scheduler, hilo async
     * sin contexto propagado, startup). En esos casos el caller debe capturar la
     * IP en el controller y pasarla explícitamente al service.
     * </p>
     */
    public static String currentIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return resolve(attrs.getRequest());
    }

    /**
     * User-Agent del request actual, o {@code null} si no hay request activo.
     * Complemento de {@link #currentIp()} para auditoría completa.
     */
    public static String currentUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String ua = attrs.getRequest().getHeader("User-Agent");
        return ua != null && !ua.isBlank() ? ua : null;
    }
}
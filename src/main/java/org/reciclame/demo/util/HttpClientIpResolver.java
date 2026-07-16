package org.reciclame.demo.util;

import jakarta.servlet.http.HttpServletRequest;

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
}
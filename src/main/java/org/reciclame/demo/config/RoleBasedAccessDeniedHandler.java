package org.reciclame.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleBasedAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            String redirectUrl = resolveHomeByRole(auth);
            response.sendRedirect(request.getContextPath() + redirectUrl);
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private String resolveHomeByRole(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_SUPERADMIN":    return "/superadmin/inicio";
                case "ROLE_ADMINISTRADOR": return "/admin/inicio";
                case "ROLE_SOCIO":         return "/socio/inicio";
                case "ROLE_VISUALIZADOR":  return "/visualizador/normativo";
                case "ROLE_IMPERSONATOR":  return "/socio/inicio";
            }
        }
        return "/login";
    }
}

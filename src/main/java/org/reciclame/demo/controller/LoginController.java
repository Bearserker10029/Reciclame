package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

@Controller
public class LoginController {

    @Component
    public static class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws java.io.IOException {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            String redirectUrl = "/admin/inicio";

            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                if (role.equals("ROLE_SUPERADMIN")) {
                    redirectUrl = "/superadmin/inicio";
                    break;
                } else if (role.equals("ROLE_ADMINISTRADOR")) {
                    redirectUrl = "/admin/inicio";
                    break;
                } else if (role.equals("ROLE_SOCIO")) {
                    redirectUrl = "/socio/inicio";
                    break;
                } else if (role.equals("ROLE_VISUALIZADOR")) {
                    redirectUrl = "/visualizador/inicio";
                    break;
                }
            }

            response.sendRedirect(redirectUrl);
        }
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login?logout=true";
    }
}
package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

@Controller
public class LoginController {

    @Component
    public static class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        private final UsuarioRepository usuarioRepository;

        public RoleBasedSuccessHandler(UsuarioRepository usuarioRepository) {
            this.usuarioRepository = usuarioRepository;
        }

        @Override
        @Transactional(readOnly = true)
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws java.io.IOException {
            // Guardar datos del usuario en sesión
            HttpSession session = request.getSession();
            usuarioRepository.findByCorreoCorporativo(authentication.getName())
                    .ifPresent(u -> session.setAttribute("usuarioSesion", new UsuarioSesion(
                            u.getId(),
                            u.getNombreCompleto(),
                            u.getCorreoCorporativo(),
                            u.getRol() != null ? u.getRol().getNombre() : null,
                            u.getCargo(),
                            u.getFotoUrl()
                    )));

            // Si el usuario venía de una URL protegida, redirigir allí primero
            DefaultSavedRequest savedRequest = (DefaultSavedRequest)
                    session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                String targetUrl = savedRequest.getRedirectUrl();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }

            // Redirección según rol
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
                    redirectUrl = "/visualizador/normativo";
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

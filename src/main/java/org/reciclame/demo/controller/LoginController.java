package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

@Controller
public class LoginController {

    /**
     * URL canónico del login. Sigue el patrón /service/<nombre>/login que se
     * alinea con el menú "Ejes de Gestión +" del sitio público
     * (reciclame.org → /service/conocimiento/, /service/educacion/, etc.).
     */
    public static final String LOGIN_PATH = "/service/repositorio/login";

    /**
     * URL del sitio público (WordPress). Configurable vía
     * {@code app.public-site-url} en application.properties.
     * Default: {@code https://reciclame.org/}. Se usa como fallback cuando no
     * se puede servir la página local (/inicio).
     */
    @Value("${app.public-site-url:https://reciclame.org/}")
    private String publicSiteUrl;

    /**
     * Si es {@code true} (default), la raíz "/" redirige a la página local
     * "/inicio" (mirror local del sitio público). Si es {@code false}, redirige
     * al sitio público de WordPress (configurado en {@link #publicSiteUrl}).
     */
    @Value("${app.use-local-public-site:true}")
    private boolean useLocalPublicSite;

    private final EstudioService estudioService;
    private final NormativaService normativaService;

    public LoginController(EstudioService estudioService, NormativaService normativaService) {
        this.estudioService = estudioService;
        this.normativaService = normativaService;
    }

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
                    .ifPresent(u -> {
                        u.setUltimoLogin(Instant.now());
                        usuarioRepository.save(u);

                        session.setAttribute("usuarioSesion", new UsuarioSesion(
                                u.getId(),
                                u.getNombreCompleto(),
                                u.getCorreoCorporativo(),
                                u.getRol() != null ? u.getRol().getNombre() : null,
                                u.getCargo(),
                                u.getFotoUrl()
                        ));
                    });

            // Si el usuario venía de una URL protegida válida, redirigir allí
            DefaultSavedRequest savedRequest = (DefaultSavedRequest)
                    session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                String targetUrl = savedRequest.getRedirectUrl();
                if (targetUrl != null
                        && !targetUrl.contains("/error")
                        && !targetUrl.contains(LOGIN_PATH)
                        && !targetUrl.contains("/login")) {
                    getRedirectStrategy().sendRedirect(request, response, targetUrl);
                    return;
                }
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
                    redirectUrl = "/visualizador/inicio";
                    break;
                }
            }

            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * Root: redirige (302) a la página pública local "/inicio" (mirror local
     * del sitio WordPress). Si {@code app.use-local-public-site=false}, redirige
     * al sitio público de WordPress.
     *
     * Si el usuario ya está autenticado y quiere entrar al backend,
     * puede navegar directamente a /service/repositorio/login o a su home por rol.
     */
    @GetMapping("/")
    public RedirectView index() {
        if (useLocalPublicSite) {
            return new RedirectView("/inicio");
        }
        String target = (publicSiteUrl != null && !publicSiteUrl.isBlank())
                ? publicSiteUrl
                : "https://reciclame.org/";
        return new RedirectView(target);
    }

    /**
     * Login canónico. Misma vista que antes, pero con URL alineado al patrón
     * del menú "Ejes de Gestión +" del sitio público.
     *
     * Carga stats reales desde la BD para alimentar las tarjetas de la
     * landing de login (totalDocumentos, totalNormativas, porcentajeActualizado).
     * Si los servicios fallan, se renderizan ceros sin tumbar la página.
     */
    @GetMapping("/service/repositorio/login")
    public String login(Model model) {
        long totalNormativas = 0;
        long totalDocumentos = 0;
        long porcentajeActualizado = 0;
        try {
            var normativas = normativaService.listarNormativas();
            var estudios = estudioService.listarEstudios();
            totalNormativas = normativas != null ? normativas.size() : 0;
            int totalEstudios = estudios != null ? estudios.size() : 0;
            totalDocumentos = totalNormativas + totalEstudios;
            if (totalDocumentos > 0) {
                long conArchivo = 0;
                if (normativas != null) {
                    conArchivo += normativas.stream()
                            .filter(n -> n.getArchivoUrl() != null && !n.getArchivoUrl().isBlank())
                            .count();
                }
                if (estudios != null) {
                    conArchivo += estudios.stream()
                            .filter(e -> e.getArchivoUrl() != null && !e.getArchivoUrl().isBlank())
                            .count();
                }
                porcentajeActualizado = Math.round((double) conArchivo * 100.0 / totalDocumentos);
            }
        } catch (Exception e) {
            System.err.println("[LoginController] No se pudieron cargar stats: " + e.getMessage());
        }
        model.addAttribute("totalDocumentos", totalDocumentos);
        model.addAttribute("totalNormativas", totalNormativas);
        model.addAttribute("porcentajeActualizado", porcentajeActualizado);
        return "login";
    }

    /**
     * Alias retrocompatible: /login sigue funcionando pero redirige al canónico.
     */
    @GetMapping("/login")
    public String loginLegacy() {
        return "redirect:" + LOGIN_PATH;
    }

    /**
     * Logout: redirige al login canónico.
     */
    @GetMapping("/logout")
    public String logout() {
        return "redirect:" + LOGIN_PATH + "?logout=true";
    }
}

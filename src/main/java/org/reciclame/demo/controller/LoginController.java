package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
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

    public static final String LOGIN_PATH = "/service/repositorio/login";

    @Value("${app.public-site-url:https://reciclame.org/}")
    private String publicSiteUrl;

    @Value("${app.use-local-public-site:true}")
    private boolean useLocalPublicSite;

    private final EstudioService estudioService;
    private final NormativaService normativaService;

    public LoginController(EstudioService estudioService, NormativaService normativaService) {
        this.estudioService = estudioService;
        this.normativaService = normativaService;
    }

    // Handler custom porque Spring Security por default no sabe mandar a cada
    // rol a su propio /inicio, ni resetear intentos fallidos/bloqueo al
    // loguear bien, ni forzar el cambio de password inicial antes de dejar
    // pasar a cualquier otra parte del sistema.
    @Component
    public static class RoleBasedSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

        private final UsuarioRepository usuarioRepository;
        private final AuditoriaService auditoriaService;

        public RoleBasedSuccessHandler(UsuarioRepository usuarioRepository,
                                      AuditoriaService auditoriaService) {
            this.usuarioRepository = usuarioRepository;
            this.auditoriaService = auditoriaService;
        }

        @Override
        @Transactional
        public void onAuthenticationSuccess(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws java.io.IOException {
            HttpSession session = request.getSession();
            boolean[] debeCambiarPassword = {false};
            usuarioRepository.findByCorreoCorporativo(authentication.getName())
                    .ifPresent(u -> {
                        debeCambiarPassword[0] = Boolean.TRUE.equals(u.getDebeCambiarPassword());
                        u.setUltimoLogin(Instant.now());
                        usuarioRepository.save(u);

                        if ((u.getIntentosFallidos() != null && u.getIntentosFallidos() != 0)
                                || Boolean.TRUE.equals(u.getCuentaBloqueada())) {
                            usuarioRepository.resetearIntentosYDesbloquear(u.getId());
                            u.setIntentosFallidos((byte) 0);
                            u.setCuentaBloqueada(false);
                        }

                        session.setAttribute("usuarioSesion", new UsuarioSesion(
                                u.getId(),
                                u.getNombreCompleto(),
                                u.getCorreoCorporativo(),
                                u.getRol() != null ? u.getRol().getNombre() : null,
                                u.getCargo(),
                                u.getFotoUrl(),
                                Boolean.TRUE.equals(u.getDebeCambiarPassword())
                        ));

                        // Guarda quién es "realmente" el SuperAdmin antes de que use
                        // Impersonación — así, al salir de la impersonación, sabemos
                        // a quién devolver la sesión sin tener que loguearse de nuevo.
                        if (u.getRol() != null && "SUPERADMIN".equalsIgnoreCase(u.getRol().getNombre())) {
                            session.setAttribute("SUPERADMIN_ORIGINAL_ID", u.getId());
                            session.setAttribute("SUPERADMIN_ORIGINAL_NOMBRE", u.getNombreCompleto());
                            session.setAttribute("SUPERADMIN_ORIGINAL_CORREO", u.getCorreoCorporativo());
                        }

                        String ip = extractClientIp(request);
                        String userAgent = request.getHeader("User-Agent");
                        String rolNombre = u.getRol() != null ? u.getRol().getNombre() : "USUARIO";
                        auditoriaService.registrarAccion(
                                u.getId(),
                                u.getNombreCompleto(),
                                AuditoriaService.ACCION_LOGIN,
                                "AUTENTICACION",
                                "Inicio de sesión exitoso (" + rolNombre + ")",
                                null, null, "EXITOSO", null, ip, userAgent
                        );
                    });

            DefaultSavedRequest savedRequest = (DefaultSavedRequest)
                    session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST");

            if (debeCambiarPassword[0]) {
                response.sendRedirect("/auth/cambiar-password-inicial");
                return;
            }

            // Volver a la página que el usuario quería ver antes de que lo mandaran
            // a loguearse — salvo que esa página guardada sea justo el login o un
            // error, porque ahí entraríamos en loop de redirects.
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

            response.sendRedirect(resolveHomeUrlForRole(authentication));
        }

        private static String extractClientIp(HttpServletRequest request) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int coma = xff.indexOf(',');
                return (coma > 0 ? xff.substring(0, coma) : xff).trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) return realIp.trim();
            return request.getRemoteAddr();
        }
    }

    private static String resolveHomeUrlForRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_SUPERADMIN")) {
                return "/superadmin/inicio";
            } else if (role.equals("ROLE_ADMINISTRADOR")) {
                return "/admin/inicio";
            } else if (role.equals("ROLE_SOCIO")) {
                return "/socio/inicio";
            } else if (role.equals("ROLE_VISUALIZADOR")) {
                return "/visualizador/inicio";
            }
        }
        return "/admin/inicio";
    }

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

    @GetMapping("/service/repositorio/login")
    public String login(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object sesion = session.getAttribute("usuarioSesion");
            if (sesion instanceof UsuarioSesion us && us.isDebeCambiarPassword()) {
                return "redirect:/auth/cambiar-password-inicial";
            }
            return "redirect:" + resolveHomeUrlForRole(authentication);
        }

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

    @GetMapping("/login")
    public String loginLegacy() {
        return "redirect:" + LOGIN_PATH;
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:" + LOGIN_PATH + "?logout=true";
    }


    @Component
    public static class AuditingLogoutHandler implements LogoutHandler {

        private final UsuarioRepository usuarioRepository;
        private final AuditoriaService auditoriaService;

        public AuditingLogoutHandler(UsuarioRepository usuarioRepository,
                                     AuditoriaService auditoriaService) {
            this.usuarioRepository = usuarioRepository;
            this.auditoriaService = auditoriaService;
        }

        @Override
        @Transactional
        public void logout(jakarta.servlet.http.HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response,
                          org.springframework.security.core.Authentication authentication) {
            if (authentication == null) {
                return;
            }
            usuarioRepository.findByCorreoCorporativo(authentication.getName())
                    .ifPresent(u -> {
                        String ip = extractClientIpStatic(request);
                        String userAgent = request.getHeader("User-Agent");
                        String rolNombre = u.getRol() != null ? u.getRol().getNombre() : "USUARIO";
                        auditoriaService.registrarAccion(
                                u.getId(),
                                u.getNombreCompleto(),
                                AuditoriaService.ACCION_LOGOUT,
                                "AUTENTICACION",
                                "Cierre de sesión (" + rolNombre + ")",
                                null, null, "EXITOSO", null, ip, userAgent
                        );
                    });
        }
    }

    private static String extractClientIpStatic(jakarta.servlet.http.HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int coma = xff.indexOf(',');
            return (coma > 0 ? xff.substring(0, coma) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}

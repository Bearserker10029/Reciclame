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
            // Guardar datos del usuario en sesión
            HttpSession session = request.getSession();
            boolean[] debeCambiarPassword = {false};
            usuarioRepository.findByCorreoCorporativo(authentication.getName())
                    .ifPresent(u -> {
                        debeCambiarPassword[0] = Boolean.TRUE.equals(u.getDebeCambiarPassword());
                        u.setUltimoLogin(Instant.now());
                        usuarioRepository.save(u);

                        // Si el usuario tenía intentos fallidos o estaba bloqueado,
                        // resetearlos al iniciar sesión correctamente.
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

                        // Si el usuario que se loguea es SuperAdmin, persistir
                        // su id en un atributo inmutable de sesión. Esto es
                        // crítico para que los registros de auditoría de
                        // IMPERSONAR / SALIR_IMPERSONACION siempre muestren
                        // el nombre real del superadmin raíz y no el correo
                        // del último usuario impersonado (que es lo que
                        // devuelve SecurityContextHolder.getName() durante
                        // una impersonación anidada).
                        if (u.getRol() != null && "SUPERADMIN".equalsIgnoreCase(u.getRol().getNombre())) {
                            session.setAttribute("SUPERADMIN_ORIGINAL_ID", u.getId());
                            session.setAttribute("SUPERADMIN_ORIGINAL_NOMBRE", u.getNombreCompleto());
                            session.setAttribute("SUPERADMIN_ORIGINAL_CORREO", u.getCorreoCorporativo());
                        }

                        // ── Trazabilidad: registrar el LOGIN ──────────────
                        // Módulo "AUTENTICACION" (no "SEGURIDAD") a propósito:
                        // la query del admin excluye modulo='SEGURIDAD', pero
                        // SÍ debe ver los logins de admin/socio/visualizador.
                        // El superadmin lo ve via findEventosSeguridad (que
                        // acepta LOGIN por acción, independientemente del módulo).
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

            // Si el usuario venía de una URL protegida válida, redirigir allí
            DefaultSavedRequest savedRequest = (DefaultSavedRequest)
                    session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            session.removeAttribute("SPRING_SECURITY_SAVED_REQUEST");

            if (debeCambiarPassword[0]) {
                // Contraseña temporal: ignora cualquier URL guardada y fuerza el
                // cambio antes de dejarlo entrar a cualquier otra pantalla.
                response.sendRedirect("/auth/cambiar-password-inicial");
                return;
            }

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
            response.sendRedirect(resolveHomeUrlForRole(authentication));
        }

        /**
         * Devuelve la IP del cliente. Prioriza {@code X-Forwarded-For}
         * (típico cuando se está detrás de nginx/ALB) y cae al
         * {@code X-Real-IP} y finalmente a {@code request.getRemoteAddr()}.
         */
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

    /**
     * URL de inicio según el rol del usuario autenticado. Compartido entre el
     * success handler (login recién hecho) y el chequeo de sesión activa en
     * {@link #login(Model)} (usuario ya autenticado que vuelve a /login).
     */
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
    public String login(Model model, HttpSession session) {
        // Si ya hay una sesión autenticada en este navegador (p.ej. otra pestaña),
        // no se debe mostrar el formulario de login: se redirige directo al home
        // del usuario. Evita la falsa impresión de "dos sesiones" cuando en
        // realidad es la misma sesión (misma cookie) vista desde otra pestaña.
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

    // ══════════════════════════════════════════════════════════════════
    //  AUDITORÍA DE LOGOUT
    // ══════════════════════════════════════════════════════════════════

    /**
     * {@link LogoutHandler} que registra un evento {@code LOGOUT} en la tabla
     * de auditoría antes de que Spring Security invalide la sesión. Se enchufa
     * en la cadena de logout vía {@code SecurityConfig.logout(...)} con
     * {@code .addLogoutHandler(auditLogoutHandler)}; corre antes que
     * {@code SecurityContextLogoutHandler} (que limpia la sesión), por lo que
     * todavía podemos leer el {@code Authentication} de
     * {@link SecurityContextHolder}.
     *
     * <p>El handler retorna el resultado de la última operación: nunca lanza
     * para no romper el flujo de logout si la BD está caída — solo loguea el
     * error y sigue.</p>
     */
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
                // Logout disparado sin sesión activa (raro pero posible); no
                // podemos atribuirlo a nadie. No registramos evento.
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

    /**
     * Helper estático de extracción de IP (mismo criterio que el success
     * handler: priorizar X-Forwarded-For, fallback a X-Real-IP y finalmente
     * a {@code request.getRemoteAddr()}). Vive en la clase externa para
     * poder ser usado por el {@link AuditingLogoutHandler} sin instanciar
     * el handler de login.
     */
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

package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.reciclame.demo.service.QrLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Endpoints para el login por QR.
 *
 * <ul>
 *   <li>{@code GET /api/perfil/qr-token} — usuario logueado pide un token; se devuelve
 *       la URL completa que se debe codificar en el QR.</li>
 *   <li>{@code GET /auth/qr?token=...} — público, abierto en el navegador del celular:
 *       valida el token, crea la sesión y redirige al home del rol.</li>
 * </ul>
 */
@RestController
public class QrLoginController {

    private static final Logger log = LoggerFactory.getLogger(QrLoginController.class);

    private final QrLoginService qrLoginService;
    private final UsuarioRepository usuarioRepo;

    public QrLoginController(QrLoginService qrLoginService, UsuarioRepository usuarioRepo) {
        this.qrLoginService = qrLoginService;
        this.usuarioRepo = usuarioRepo;
    }

    /**
     * Devuelve un token y la URL absoluta que el frontend debe pintar en el QR.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/perfil/qr-token")
    public ResponseEntity<Map<String, Object>> generarToken(
            @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails ud,
            HttpServletRequest request) {
        if (ud == null) return ResponseEntity.status(401).build();
        Optional<Usuario> opt = usuarioRepo.findByCorreoCorporativo(ud.getUsername());
        if (opt.isEmpty()) return ResponseEntity.status(404).build();

        String token = qrLoginService.generarToken(opt.get().getId());
        String baseUrl = buildBaseUrl(request);
        String qrUrl = baseUrl + "/auth/qr?token=" + token;

        return ResponseEntity.ok(Map.of(
                "token", token,
                "url", qrUrl,
                "expiraEnSegundos", 300
        ));
    }

    /**
     * Endpoint público al que apunta el QR. Crea la sesión y redirige al home del rol.
     * Si el token es inválido/expirado, redirige al login con un parámetro de error.
     */
    @GetMapping("/auth/qr")
    public RedirectView loginPorQr(@RequestParam("token") String token,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        Optional<Long> userIdOpt = qrLoginService.consumirToken(token);
        if (userIdOpt.isEmpty()) {
            log.warn("Intento de login con QR inválido o expirado desde IP {}", request.getRemoteAddr());
            return new RedirectView("/service/repositorio/login?qrError=1");
        }

        Optional<Usuario> usuarioOpt = usuarioRepo.findById(userIdOpt.get());
        if (usuarioOpt.isEmpty()) {
            return new RedirectView("/service/repositorio/login?qrError=2");
        }
        Usuario u = usuarioOpt.get();

        String rolNombre = u.getRol() != null ? u.getRol().getNombre() : "USUARIO";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rolNombre));

        User principal = new User(u.getCorreoCorporativo(), "", authorities);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authToken.setDetails(new WebAuthenticationDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
        // Persistir el SecurityContext en la sesión (Spring Security 6)
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);

        // Datos en sesión que el topbar/sidebar consume
        HttpSession session = request.getSession();
        session.setAttribute("usuarioSesion", new UsuarioSesion(
                u.getId(),
                u.getNombreCompleto(),
                u.getCorreoCorporativo(),
                rolNombre,
                u.getCargo(),
                u.getFotoUrl()
        ));

        // Actualizar último login
        try {
            u.setUltimoLogin(Instant.now());
            usuarioRepo.save(u);
        } catch (Exception e) {
            log.warn("No se pudo actualizar ultimoLogin tras login QR: {}", e.getMessage());
        }

        String redirectUrl = switch (rolNombre) {
            case "SUPERADMIN" -> "/superadmin/inicio";
            case "ADMINISTRADOR" -> "/admin/inicio";
            case "SOCIO" -> "/socio/inicio";
            case "VISUALIZADOR" -> "/visualizador/inicio";
            default -> "/admin/inicio";
        };
        log.info("Login QR exitoso para usuario {} (rol={})", u.getCorreoCorporativo(), rolNombre);
        return new RedirectView(redirectUrl);
    }

    private String buildBaseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) proto = request.getScheme();
        else proto = proto.split(",")[0].trim();

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            int port = request.getServerPort();
            host = request.getServerName()
                    + (("http".equals(proto) && port == 80) || ("https".equals(proto) && port == 443)
                       ? "" : ":" + port);
        } else {
            host = host.split(",")[0].trim();
        }
        return proto + "://" + host;
    }
}

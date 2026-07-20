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

// Login por QR: el usuario ya logueado en un dispositivo pide un token de
// un solo uso (generarToken), lo muestra como QR, y otro dispositivo lo
// "canjea" en /auth/qr para loguearse sin escribir contraseña. Como no hay
// password de por medio, acá se arma el Authentication a mano en vez de
// pasar por el AuthenticationManager normal — la confianza viene de que el
// token ya identifica al usuario de forma única y expira solo.
@RestController
public class QrLoginController {

    private static final Logger log = LoggerFactory.getLogger(QrLoginController.class);

    private final QrLoginService qrLoginService;
    private final UsuarioRepository usuarioRepo;

    public QrLoginController(QrLoginService qrLoginService, UsuarioRepository usuarioRepo) {
        this.qrLoginService = qrLoginService;
        this.usuarioRepo = usuarioRepo;
    }

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
                "expiraEnSegundos", qrLoginService.obtenerMinutosExpiracion() * 60
        ));
    }

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
        new HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);

        HttpSession session = request.getSession();
        session.setAttribute("usuarioSesion", new UsuarioSesion(
                u.getId(),
                u.getNombreCompleto(),
                u.getCorreoCorporativo(),
                rolNombre,
                u.getCargo(),
                u.getFotoUrl(),
                Boolean.TRUE.equals(u.getDebeCambiarPassword())
        ));

        try {
            u.setUltimoLogin(Instant.now());
            usuarioRepo.save(u);
        } catch (Exception e) {
            log.warn("No se pudo actualizar ultimoLogin tras login QR: {}", e.getMessage());
        }

        String redirectUrl;
        if (Boolean.TRUE.equals(u.getDebeCambiarPassword())) {
            redirectUrl = "/auth/cambiar-password-inicial";
        } else {
            redirectUrl = switch (rolNombre) {
                case "SUPERADMIN" -> "/superadmin/inicio";
                case "ADMINISTRADOR" -> "/admin/inicio";
                case "SOCIO" -> "/socio/inicio";
                case "VISUALIZADOR" -> "/visualizador/inicio";
                default -> "/admin/inicio";
            };
        }
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

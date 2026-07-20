package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.service.AuthService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public PasswordResetController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    // Mensaje siempre igual exista o no el correo, a propósito: si decimos
    // "correo no encontrado" alguien podría usar este endpoint para enumerar
    // qué correos están registrados.
    @PostMapping("/recuperar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String correo = body.getOrDefault("correo", "").trim();
        if (correo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo requerido"));
        }
        authService.solicitarRecuperacion(correo);
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo existe, recibirás el enlace en breve."));
    }

    @GetMapping("/restablecer")
    public String formularioRestablecimiento(@RequestParam String token,
                                             Model model,
                                             RedirectAttributes ra) {
        if (authService.validarToken(token).isEmpty()) {
            ra.addFlashAttribute("errorRecovery", "El enlace ha expirado o ya fue utilizado. Solicita uno nuevo.");
            return "redirect:/service/repositorio/login";
        }
        model.addAttribute("token", token);
        return "auth/restablecer-contrasena";
    }

    @PostMapping("/restablecer")
    public String restablecerContrasena(@RequestParam String token,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword,
                                        RedirectAttributes ra) {
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/auth/restablecer?token=" + token;
        }
        if (password.length() < 8) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/auth/restablecer?token=" + token;
        }
        try {
            authService.restablecerContrasena(token, password);
            ra.addFlashAttribute("exitoRecovery", "Contraseña restablecida. Ya puedes iniciar sesión.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorRecovery", "El enlace ha expirado o ya fue utilizado.");
        }
        return "redirect:/service/repositorio/login";
    }


    @GetMapping("/cambiar-password-inicial")
    public String formularioCambioInicial(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null) return "redirect:/service/repositorio/login";
        model.addAttribute("correo", user.getUsername());
        return "auth/cambiar-password-inicial";
    }

    @PostMapping("/cambiar-password-inicial")
    public String cambiarPasswordInicial(@RequestParam String password,
                                         @RequestParam String confirmPassword,
                                         @AuthenticationPrincipal UserDetails user,
                                         HttpSession session,
                                         RedirectAttributes ra) {
        if (user == null) return "redirect:/service/repositorio/login";

        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/auth/cambiar-password-inicial";
        }
        if (password.length() < 8) {
            ra.addFlashAttribute("error", "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/auth/cambiar-password-inicial";
        }

        var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
        if (usuarioOpt.isEmpty()) return "redirect:/service/repositorio/login";
        var usuario = usuarioOpt.get();

        authService.cambiarPasswordInicial(usuario.getId(), password);

        // Actualizar también el flag en la sesión que ya está viva, si no,
        // el usuario sigue viendo el modal forzado de "cambia tu contraseña"
        // hasta que vuelva a loguearse.
        Object sesionAttr = session.getAttribute("usuarioSesion");
        if (sesionAttr instanceof UsuarioSesion us) {
            us.setDebeCambiarPassword(false);
        }

        String home = switch (usuario.getRolNombre() != null ? usuario.getRolNombre() : "") {
            case "SUPERADMIN" -> "/superadmin/inicio";
            case "ADMINISTRADOR" -> "/admin/inicio";
            case "SOCIO" -> "/socio/inicio";
            case "VISUALIZADOR" -> "/visualizador/inicio";
            default -> "/admin/inicio";
        };
        ra.addFlashAttribute("exitoPassword", "Contraseña actualizada. ¡Bienvenido!");
        return "redirect:" + home;
    }
}

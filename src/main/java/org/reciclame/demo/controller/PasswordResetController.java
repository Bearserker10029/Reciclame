package org.reciclame.demo.controller;

import org.reciclame.demo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {

    private final AuthService authService;

    public PasswordResetController(AuthService authService) {
        this.authService = authService;
    }

    // ── AJAX desde login.html ─────────────────────────────────────────────
    @PostMapping("/recuperar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@RequestBody Map<String, String> body) {
        String correo = body.getOrDefault("correo", "").trim();
        if (correo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Correo requerido"));
        }
        authService.solicitarRecuperacion(correo);
        // Respuesta genérica: nunca revelamos si el correo existe
        return ResponseEntity.ok(Map.of("mensaje", "Si el correo existe, recibirás el enlace en breve."));
    }

    // ── Formulario de nueva contraseña ────────────────────────────────────
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
}

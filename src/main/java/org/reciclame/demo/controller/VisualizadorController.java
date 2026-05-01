package org.reciclame.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VisualizadorController {

    private void setupVisualizadorAttributes(Model model, UserDetails user) {
        model.addAttribute("rol", "VISUALIZADOR");
        model.addAttribute("rolNombre", "Visualizador");
        model.addAttribute("usuarioNombre", user != null ? user.getUsername() : "Visualizador");
        model.addAttribute("notifUrl", "/visualizador/notificaciones");
    }

    @GetMapping({"/visualizador", "/visualizador/inicio"})
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        return "Socio/inicio";
    }

    @GetMapping("/visualizador/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        return "Socio/inicio";
    }

    @GetMapping("/visualizador/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        return "Socio/inicio";
    }
}
package org.reciclame.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SocioController {

    @GetMapping("/socio")
    public String inicio(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/inicio";
    }

    @GetMapping("/socio/inicio")
    public String socioInicio(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/inicio";
    }

    @GetMapping("/socio/notificaciones")
    public String notificaciones(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/notificaciones";
    }

    @GetMapping("/socio/perfil")
    public String perfil(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/perfil";
    }

    @GetMapping("/socio/normativo")
    public String normativo(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/normativo";
    }

    @GetMapping("/socio/estudios")
    public String estudios(Model model) {
        model.addAttribute("rol", "SOCIO");
        return "socio/estudios";
    }
}
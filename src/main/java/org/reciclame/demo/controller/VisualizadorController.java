package org.reciclame.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class VisualizadorController {

    @GetMapping("/visualizador")
    public String inicio(Model model) {
        model.addAttribute("rol", "VISUALIZADOR");
        return "visualizador/estudios";
    }

    @GetMapping("/visualizador/normativo")
    public String normativo(Model model) {
        model.addAttribute("rol", "VISUALIZADOR");
        return "visualizador/estudios";
    }

    @GetMapping("/visualizador/estudios")
    public String estudios(Model model) {
        model.addAttribute("rol", "VISUALIZADOR");
        return "visualizador/estudios";
    }
}
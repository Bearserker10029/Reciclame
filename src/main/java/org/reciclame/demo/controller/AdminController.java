package org.reciclame.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String inicio(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/inicio";
    }

    @GetMapping("/admin/inicio")
    public String adminInicio(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/inicio";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/usuarios";
    }

    @GetMapping("/admin/documentos")
    public String documentos(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/documentos";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/reportes";
    }

    @GetMapping("/admin/trazabilidad")
    public String trazabilidad(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/trazabilidad";
    }

    @GetMapping("/admin/notificaciones")
    public String notificaciones(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/notificaciones";
    }

    @GetMapping("/admin/perfil")
    public String perfil(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/perfil";
    }

    @GetMapping("/admin/normativo")
    public String normativo(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/normativo";
    }

    @GetMapping("/admin/estudios")
    public String estudios(Model model) {
        model.addAttribute("rol", "ADMIN");
        return "admin/estudios";
    }
}
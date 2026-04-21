package org.reciclame.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SuperAdminController {

    @GetMapping("/superadmin")
    public String inicio(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/inicio";
    }

    @GetMapping("/superadmin/inicio")
    public String superAdminInicio(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/inicio";
    }

    @GetMapping("/superadmin/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/usuarios";
    }

    @GetMapping("/superadmin/documentos")
    public String documentos(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/documentos";
    }

    @GetMapping("/superadmin/reportes")
    public String reportes(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/reportes";
    }

    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/trazabilidad";
    }

    @GetMapping("/superadmin/notificaciones")
    public String notificaciones(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/notificaciones";
    }

    @GetMapping("/superadmin/perfil")
    public String perfil(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/perfil";
    }

    @GetMapping("/superadmin/normativo")
    public String normativo(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/normativo";
    }

    @GetMapping("/superadmin/estudios")
    public String estudios(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/estudios";
    }

    @GetMapping("/superadmin/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/auditoria";
    }

    @GetMapping("/superadmin/impersonar")
    public String impersonar(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        return "superadmin/impersonar";
    }
}
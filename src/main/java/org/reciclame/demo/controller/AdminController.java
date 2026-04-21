package org.reciclame.demo.controller;

import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;

    public AdminController(UsuarioService usuarioService, NormativaService normativaService, EstudioService estudioService) {
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
    }

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
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        return "admin/usuarios";
    }

    @GetMapping("/admin/documentos")
    public String documentos(Model model) {
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();

        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        model.addAttribute("rol", "ADMIN");
        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("totalDescargas", totalDescargas);
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
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "admin/normativo";
    }

    @GetMapping("/admin/estudios")
    public String estudios(Model model) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("estudios", estudioService.listarEstudios());
        return "admin/estudios";
    }
}
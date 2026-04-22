package org.reciclame.demo.controller;

import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SuperAdminController {

    private final UsuarioService usuarioService;

    public SuperAdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        usuarioService.findByCorreo(correo).ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    @GetMapping("/superadmin")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/inicio";
    }

    @GetMapping("/superadmin/inicio")
    public String superAdminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/inicio";
    }

    @GetMapping("/superadmin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/usuarios";
    }

    @GetMapping("/superadmin/documentos")
    public String documentos(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/documentos";
    }

    @GetMapping("/superadmin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/reportes";
    }

    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/trazabilidad";
    }

    @GetMapping("/superadmin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/notificaciones";
    }

    @GetMapping("/superadmin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/perfil";
    }

    @GetMapping("/superadmin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/normativo";
    }

    @GetMapping("/superadmin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/estudios";
    }

    @GetMapping("/superadmin/auditoria")
    public String auditoria(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/auditoria";
    }

    @GetMapping("/superadmin/impersonar")
    public String impersonar(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "superadmin/impersonar";
    }
}
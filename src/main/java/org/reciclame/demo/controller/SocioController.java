package org.reciclame.demo.controller;

import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SocioController {

    private final UsuarioService usuarioService;
    private final EstudioService estudioService;
    private final NormativaService normativaService;
    private final NotificacionService notificacionService;

    public SocioController(UsuarioService usuarioService,
                          EstudioService estudioService,
                          NormativaService normativaService,
                          NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.estudioService = estudioService;
        this.normativaService = normativaService;
        this.notificacionService = notificacionService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        usuarioService.findByCorreo(correo).ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    @GetMapping("/socio")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "socio/inicio";
    }

    @GetMapping("/socio/inicio")
    public String socioInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "socio/inicio";
    }

    @GetMapping("/socio/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "socio/notificaciones";
    }

    @GetMapping("/socio/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "socio/perfil";
    }

    @GetMapping("/socio/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "socio/normativo";
    }

    @GetMapping("/socio/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        return "socio/estudios";
    }
}
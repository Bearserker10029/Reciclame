package org.reciclame.demo.controller;

import org.reciclame.demo.service.EtiquetaService;
import org.reciclame.demo.service.EstadosNormativaService;
import org.reciclame.demo.service.TematicaService;
import org.reciclame.demo.service.TiposNormativaService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiccionariosController {

    private final TematicaService tematicaService;
    private final EtiquetaService etiquetaService;
    private final TiposNormativaService tiposNormativaService;
    private final EstadosNormativaService estadosNormativaService;
    private final UsuarioService usuarioService;

    public DiccionariosController(TematicaService tematicaService,
                                   EtiquetaService etiquetaService,
                                   TiposNormativaService tiposNormativaService,
                                   EstadosNormativaService estadosNormativaService,
                                   UsuarioService usuarioService) {
        this.tematicaService         = tematicaService;
        this.etiquetaService         = etiquetaService;
        this.tiposNormativaService   = tiposNormativaService;
        this.estadosNormativaService = estadosNormativaService;
        this.usuarioService          = usuarioService;
    }

    @GetMapping("/admin/diccionarios")
    public String diccionarios(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("rol",      "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));

        model.addAttribute("tematicas",         tematicaService.listarTematicasTodas());
        model.addAttribute("etiquetas",         etiquetaService.listarEtiquetasTodas());
        model.addAttribute("tiposNormativa",    tiposNormativaService.listarTiposTodos());
        model.addAttribute("estadosNormativa",  estadosNormativaService.listarEstadosTodos());
        return "Admin/diccionarios";
    }
}

package org.reciclame.demo.controller;

import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.service.EmpresaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.RolService;
import org.reciclame.demo.service.TematicaService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;
    private final EmpresaService empresaService;
    private final RolService rolService;
    private final TematicaService tematicaService;
    private final NotificacionService notificacionService;

    public AdminController(UsuarioService usuarioService, NormativaService normativaService,
                           EstudioService estudioService, EmpresaService empresaService,
                           RolService rolService, TematicaService tematicaService,
                           NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
        this.empresaService = empresaService;
        this.rolService = rolService;
        this.tematicaService = tematicaService;
        this.notificacionService = notificacionService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        usuarioService.findByCorreo(correo).ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    @GetMapping("/admin")
    public String inicio(Model model) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("usuarioNombre", "Admin General");
        return "admin/inicio";
    }

    @GetMapping("/admin/inicio")
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        // Datos para fragmento normativo
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());

        // Datos para fragmento estudios
        model.addAttribute("estudios", estudioService.listarEstudios());

        // Datos para fragmento usuarios
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());

        // Datos para fragmento notificaciones
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });

        // Datos para fragmento documentos
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);

        return "admin/inicio";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());
        return "admin/usuarios";
    }

    @GetMapping("/admin/documentos")
    public String documentos(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();

        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("totalDescargas", totalDescargas);
        return "admin/documentos";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "admin/reportes";
    }

    @GetMapping("/admin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "admin/trazabilidad";
    }

    @GetMapping("/admin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "admin/notificaciones";
    }

    @GetMapping("/admin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "admin/perfil";
    }

    @GetMapping("/admin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "admin/normativo";
    }

    @GetMapping("/admin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "admin/estudios";
    }
}
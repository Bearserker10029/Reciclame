package org.reciclame.demo.controller;

import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SuperAdminController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public SuperAdminController(UsuarioService usuarioService,
                                NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        model.addAttribute("notifUrl", "/superadmin/notificaciones");
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
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "superadmin/notificaciones";
    }

    @PostMapping("/superadmin/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/superadmin/notificaciones";
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

    @GetMapping("/superadmin/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId()))
        );
        model.addAttribute("configuracionUrl", "/superadmin/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/superadmin/notificaciones");
        return "configuracion-notificaciones";
    }

    @PostMapping("/superadmin/notificaciones/configuracion")
    public String guardarConfiguracionNotificaciones(
            @RequestParam(defaultValue = "false") boolean notifNuevasNormativas,
            @RequestParam(defaultValue = "false") boolean notifNuevosEstudios,
            @RequestParam(defaultValue = "false") boolean notifCambiosSeguidos,
            @RequestParam(defaultValue = "false") boolean emailHabilitado,
            @RequestParam(defaultValue = "DIARIA") String frecuenciaEmail,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                notificacionService.guardarPreferencias(u.getId(), notifNuevasNormativas,
                        notifNuevosEstudios, notifCambiosSeguidos, emailHabilitado, frecuenciaEmail)
        );
        redirectAttributes.addFlashAttribute("guardado", true);
        return "redirect:/superadmin/notificaciones/configuracion";
    }
}
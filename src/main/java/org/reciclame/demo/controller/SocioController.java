package org.reciclame.demo.controller;

import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.SocioReportesService;
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
public class SocioController {

    private final UsuarioService usuarioService;
    private final EstudioService estudioService;
    private final NormativaService normativaService;
    private final NotificacionService notificacionService;
    private final SocioReportesService socioReportesService;

    public SocioController(UsuarioService usuarioService,
                          EstudioService estudioService,
                          NormativaService normativaService,
                          NotificacionService notificacionService,
                          SocioReportesService socioReportesService) {
        this.usuarioService = usuarioService;
        this.estudioService = estudioService;
        this.normativaService = normativaService;
        this.notificacionService = notificacionService;
        this.socioReportesService = socioReportesService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        model.addAttribute("notifUrl", "/socio/notificaciones");
        try {
            usuarioService.findByCorreo(correo).ifPresent(u -> {
                model.addAttribute("usuarioNombre", u.getNombreCompleto());
                try {
                    model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
                } catch (Exception e) {
                    model.addAttribute("noLeidasCount", 0);
                }
            });
        } catch (Exception e) {
            model.addAttribute("noLeidasCount", 0);
        }
    }

    @GetMapping("/socio")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Socio/inicio";
    }

    @GetMapping("/socio/inicio")
    public String socioInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Socio/inicio";
    }

    @GetMapping("/socio/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
        });
        return "Socio/notificaciones";
    }

    @GetMapping("/socio/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "Socio/perfil";
    }

    @GetMapping("/socio/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Socio/normativo";
    }

    @GetMapping("/socio/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        return "Socio/estudios";
    }

    /**
     * Reportes personales del socio. Datos de demostración en plantilla hasta conectar KPIs reales.
     */
    @GetMapping("/socio/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresentOrElse(
                u -> model.addAttribute("reporte", socioReportesService.construir(u.getId())),
                () -> model.addAttribute("reporte", socioReportesService.construir(null))
        );
        return "Socio/reportes";
    }

    @PostMapping("/socio/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/socio/notificaciones";
    }

    @GetMapping("/socio/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId()))
        );
        model.addAttribute("configuracionUrl", "/socio/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/socio/notificaciones");
        return "configuracion-notificaciones";
    }

    @PostMapping("/socio/notificaciones/configuracion")
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
        return "redirect:/socio/notificaciones/configuracion";
    }
}
package org.reciclame.demo.controller;

import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.service.DocumentosRecientesService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.MinioStorageService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Mismo patrón que NormativaVisorController: un "ver" por rol, misma
// plantilla reutilizada. Acá además se suma 1 a las visualizaciones del
// estudio cada vez que se abre (para las estadísticas del dashboard).
@Controller
public class EstudioVisorController {

    private static final Logger log = LoggerFactory.getLogger(EstudioVisorController.class);

    private final EstudioService estudioService;
    private final MinioStorageService minioStorageService;
    private final UsuarioService usuarioService;
    private final DocumentosRecientesService documentosRecientesService;
    private final NotificacionService notificacionService;

    public EstudioVisorController(EstudioService estudioService,
                                   MinioStorageService minioStorageService,
                                   UsuarioService usuarioService,
                                   DocumentosRecientesService documentosRecientesService,
                                   NotificacionService notificacionService) {
        this.estudioService = estudioService;
        this.minioStorageService = minioStorageService;
        this.usuarioService = usuarioService;
        this.documentosRecientesService = documentosRecientesService;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/admin/estudios/{id}/ver")
    public String verEstudioAdmin(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails user) {
        setupAdmin(model, user.getUsername());
        return cargarVisor(id, model, "Admin/estudios-visor", "/admin/estudios");
    }

    @GetMapping("/superadmin/estudios/{id}/ver")
    public String verEstudioSuperAdmin(@PathVariable Long id, Model model,
                                        @AuthenticationPrincipal UserDetails user) {
        setupSuperAdmin(model, user.getUsername());
        return cargarVisor(id, model, "SuperAdmin/estudios-visor", "/superadmin/estudios");
    }

    @GetMapping("/socio/estudios/{id}/ver")
    public String verEstudioSocio(@PathVariable Long id, Model model,
                                   @RequestParam(value = "from", required = false) String from,
                                   @AuthenticationPrincipal UserDetails user) {
        setupSocio(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> documentosRecientesService.registrarVista(u.getId(), MiUnidadItem.TIPO_ESTUDIO, id));
        String urlRetorno = "mi-unidad".equals(from) ? "/socio/mi-unidad" : "/socio/estudios";
        return cargarVisor(id, model, "Socio/estudios-visor", urlRetorno);
    }

    @GetMapping("/visualizador/estudios/{id}/ver")
    public String verEstudioVisualizador(@PathVariable Long id, Model model,
                                         @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("rol", "VISUALIZADOR");
        model.addAttribute("rolNombre", "Visualizador");
        model.addAttribute("notifUrl", "/visualizador/notificaciones");
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
                documentosRecientesService.registrarVista(u.getId(), MiUnidadItem.TIPO_ESTUDIO, id);
            });
        } catch (Exception e) {
            model.addAttribute("noLeidasCount", 0);
        }
        return cargarVisorPublico(id, model, "Visualizador/estudios-visor", "/visualizador/estudios");
    }


    private String cargarVisor(Long id, Model model, String template, String urlRetorno) {
        EstudioResponseDTO estudio;
        try {
            estudio = estudioService.obtenerEstudioPorId(id);
        } catch (RuntimeException e) {
            log.warn("Estudio {} no encontrado: {}", id, e.getMessage());
            return "redirect:" + urlRetorno;
        }
        estudioService.incrementarVisualizaciones(id);
        model.addAttribute("estudio", estudio);
        poblarVisor(model, estudio.getArchivoUrl(), estudio.getTitulo(), estudio.getArchivoTipoMime());
        model.addAttribute("urlRetorno", urlRetorno);
        return template;
    }

    private String cargarVisorPublico(Long id, Model model, String template, String urlRetorno) {
        EstudioResponseDTO estudio;
        try {
            estudio = estudioService.obtenerEstudioPublicoPorId(id);
        } catch (RuntimeException e) {
            log.warn("Estudio público {} no encontrado o no accesible: {}", id, e.getMessage());
            return "redirect:" + urlRetorno;
        }
        estudioService.incrementarVisualizaciones(id);
        model.addAttribute("estudio", estudio);
        poblarVisor(model, estudio.getArchivoUrl(), estudio.getTitulo(), estudio.getArchivoTipoMime());
        model.addAttribute("urlRetorno", urlRetorno);
        return template;
    }

    private void poblarVisor(Model model, String archivoUrl, String titulo, String archivoTipoMime) {
        model.addAttribute("archivoDisponible", minioStorageService.existeArchivo(archivoUrl));
    }

    private void setupAdmin(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
        usuarioService.findByCorreo(correo).ifPresent(u -> {
            model.addAttribute("usuarioNombre", u.getNombreCompleto());
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
    }

    private void setupSuperAdmin(Model model, String correo) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        model.addAttribute("notifUrl", "/superadmin/notificaciones");
        usuarioService.findByCorreo(correo)
                .ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    private void setupSocio(Model model, String correo) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        model.addAttribute("notifUrl", "/socio/notificaciones");
        usuarioService.findByCorreo(correo)
                .ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }
}

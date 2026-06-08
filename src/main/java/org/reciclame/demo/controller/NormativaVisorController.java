package org.reciclame.demo.controller;

import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.service.DocumentosRecientesService;
import org.reciclame.demo.service.MinioStorageService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class NormativaVisorController {

    private static final Logger log = LoggerFactory.getLogger(NormativaVisorController.class);

    private final NormativaService normativaService;
    private final MinioStorageService minioStorageService;
    private final UsuarioService usuarioService;
    private final DocumentosRecientesService documentosRecientesService;
    private final NotificacionService notificacionService;

    public NormativaVisorController(NormativaService normativaService,
                                    MinioStorageService minioStorageService,
                                    UsuarioService usuarioService,
                                    DocumentosRecientesService documentosRecientesService,
                                    NotificacionService notificacionService) {
        this.normativaService = normativaService;
        this.minioStorageService = minioStorageService;
        this.usuarioService = usuarioService;
        this.documentosRecientesService = documentosRecientesService;
        this.notificacionService = notificacionService;
    }

    // ── Admin ─────────────────────────────────────────────────
    @GetMapping("/admin/normativo/{id}/ver")
    public String verNormativaAdmin(@PathVariable Long id, Model model,
                                    @AuthenticationPrincipal UserDetails user) {
        setupAdmin(model, user.getUsername());
        return cargarVisor(id, model, "Admin/normativo-visor", "/admin/normativo");
    }

    // ── Socio ─────────────────────────────────────────────────
    @GetMapping("/socio/normativo/{id}/ver")
    public String verNormativaSocio(@PathVariable Long id, Model model,
                                    @AuthenticationPrincipal UserDetails user) {
        setupSocio(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> documentosRecientesService.registrarVista(u.getId(), MiUnidadItem.TIPO_NORMATIVA, id));
        return cargarVisorPublico(id, model, "Socio/normativo-visor", "/socio/normativo");
    }

    // ── Visualizador (solo normativas públicas) ───────────────
    @GetMapping("/visualizador/normativo/{id}/ver")
    public String verNormativaVisualizador(@PathVariable Long id, Model model) {
        model.addAttribute("rol", "VISUALIZADOR");
        model.addAttribute("rolNombre", "Visualizador");
        return cargarVisorPublico(id, model, "Visualizador/normativo-visor", "/visualizador/normativo");
    }

    // ── Helpers ───────────────────────────────────────────────

    private String cargarVisor(Long id, Model model, String template, String urlRetorno) {
        NormativaResponseDTO normativa;
        try {
            normativa = normativaService.obtenerNormativaPorId(id);
        } catch (RuntimeException e) {
            log.warn("Normativa {} no encontrada: {}", id, e.getMessage());
            return "redirect:" + urlRetorno;
        }
        model.addAttribute("normativa", normativa);
        model.addAttribute("archivoDisponible", minioStorageService.existeArchivo(normativa.getArchivoUrl()));
        model.addAttribute("urlRetorno", urlRetorno);
        return template;
    }

    private String cargarVisorPublico(Long id, Model model, String template, String urlRetorno) {
        NormativaResponseDTO normativa;
        try {
            normativa = normativaService.obtenerNormativaPublicaPorId(id);
        } catch (RuntimeException e) {
            log.warn("Normativa pública {} no encontrada o no accesible: {}", id, e.getMessage());
            return "redirect:" + urlRetorno;
        }
        model.addAttribute("normativa", normativa);
        model.addAttribute("archivoDisponible", minioStorageService.existeArchivo(normativa.getArchivoUrl()));
        model.addAttribute("urlRetorno", urlRetorno);
        return template;
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

    private void setupSocio(Model model, String correo) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        model.addAttribute("notifUrl", "/socio/notificaciones");
        usuarioService.findByCorreo(correo).ifPresent(u -> {
            model.addAttribute("usuarioNombre", u.getNombreCompleto());
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
    }
}

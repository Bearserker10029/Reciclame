package org.reciclame.demo.controller;

import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.MinioStorageService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class EstudioVisorController {

    private final EstudioService estudioService;
    private final MinioStorageService minioStorageService;
    private final UsuarioService usuarioService;

    @Value("${kkfileview.url}")
    private String kkfileviewUrl;

    public EstudioVisorController(EstudioService estudioService,
                                   MinioStorageService minioStorageService,
                                   UsuarioService usuarioService) {
        this.estudioService = estudioService;
        this.minioStorageService = minioStorageService;
        this.usuarioService = usuarioService;
    }

    // ── Admin ─────────────────────────────────────────────────
    @GetMapping("/admin/estudios/{id}/ver")
    public String verEstudioAdmin(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails user) {
        setupAdmin(model, user.getUsername());
        return cargarVisor(id, model, "Admin/estudios-visor", "/admin/estudios");
    }

    // ── SuperAdmin ────────────────────────────────────────────
    @GetMapping("/superadmin/estudios/{id}/ver")
    public String verEstudioSuperAdmin(@PathVariable Long id, Model model,
                                        @AuthenticationPrincipal UserDetails user) {
        setupSuperAdmin(model, user.getUsername());
        return cargarVisor(id, model, "SuperAdmin/estudios-visor", "/superadmin/estudios");
    }

    // ── Socio (solo documentos públicos) ──────────────────────
    @GetMapping("/socio/estudios/{id}/ver")
    public String verEstudioSocio(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails user) {
        setupSocio(model, user.getUsername());
        return cargarVisorPublico(id, model, "Socio/estudios-visor", "/socio/estudios");
    }

    // ── Visualizador (solo documentos públicos) ───────────────
    @GetMapping("/visualizador/estudios/{id}/ver")
    public String verEstudioVisualizador(@PathVariable Long id, Model model) {
        model.addAttribute("rol", "VISUALIZADOR");
        model.addAttribute("rolNombre", "Visualizador");
        return cargarVisorPublico(id, model, "Visualizador/estudios-visor", "/visualizador/estudios");
    }

    // ── Helpers ───────────────────────────────────────────────

    private String cargarVisor(Long id, Model model, String template, String urlRetorno) {
        try {
            EstudioResponseDTO estudio = estudioService.obtenerEstudioPorId(id);
            estudioService.incrementarVisualizaciones(id);
            model.addAttribute("estudio", estudio);
            model.addAttribute("visorUrl", construirVisorUrl(estudio.getArchivoUrl(), estudio.getTitulo()));
            model.addAttribute("urlRetorno", urlRetorno);
            return template;
        } catch (RuntimeException e) {
            return "redirect:" + urlRetorno;
        }
    }

    private String cargarVisorPublico(Long id, Model model, String template, String urlRetorno) {
        try {
            EstudioResponseDTO estudio = estudioService.obtenerEstudioPublicoPorId(id);
            estudioService.incrementarVisualizaciones(id);
            model.addAttribute("estudio", estudio);
            model.addAttribute("visorUrl", construirVisorUrl(estudio.getArchivoUrl(), estudio.getTitulo()));
            model.addAttribute("urlRetorno", urlRetorno);
            return template;
        } catch (RuntimeException e) {
            return "redirect:" + urlRetorno;
        }
    }

    private String construirVisorUrl(String archivoUrl, String titulo) {
        if (archivoUrl == null || archivoUrl.isBlank()) return "";
        String internalUrl = minioStorageService.urlParaVisor(archivoUrl);
        String base64Url = Base64.getEncoder().encodeToString(internalUrl.getBytes(StandardCharsets.UTF_8));
        String fullfilename = URLEncoder.encode(
                sanitizarNombreArchivo(titulo) + extraerExtension(archivoUrl),
                StandardCharsets.UTF_8);
        return kkfileviewUrl + "/onlinePreview?url=" + base64Url + "&fullfilename=" + fullfilename;
    }

    // Reemplaza espacios con guion bajo y elimina caracteres inválidos en nombres de archivo
    private String sanitizarNombreArchivo(String titulo) {
        if (titulo == null || titulo.isBlank()) return "documento";
        return titulo.trim()
                .replaceAll("[/\\\\:*?\"<>|]", "")
                .replaceAll("\\s+", "_")
                .replaceAll("_{2,}", "_");
    }

    // Extrae la extensión del objectName almacenado en MinIO (ej: ".pdf", ".pptx")
    private String extraerExtension(String url) {
        try {
            String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
            int dot = path.lastIndexOf('.');
            int slash = path.lastIndexOf('/');
            if (dot > slash && dot > 0) {
                return path.substring(dot).toLowerCase();
            }
        } catch (Exception ignored) {}
        return "";
    }

    private void setupAdmin(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
        usuarioService.findByCorreo(correo)
                .ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
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
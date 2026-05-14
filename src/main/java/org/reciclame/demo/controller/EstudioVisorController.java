package org.reciclame.demo.controller;

import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.service.DocumentosRecientesService;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class EstudioVisorController {

    private static final Logger log = LoggerFactory.getLogger(EstudioVisorController.class);

    private final EstudioService estudioService;
    private final MinioStorageService minioStorageService;
    private final UsuarioService usuarioService;
    private final DocumentosRecientesService documentosRecientesService;

    @Value("${kkfileview.url}")
    private String kkfileviewUrl;

    @Value("${app.kk-accessible-url}")
    private String kkAccessibleUrl;

    public EstudioVisorController(EstudioService estudioService,
                                   MinioStorageService minioStorageService,
                                   UsuarioService usuarioService,
                                   DocumentosRecientesService documentosRecientesService) {
        this.estudioService = estudioService;
        this.minioStorageService = minioStorageService;
        this.usuarioService = usuarioService;
        this.documentosRecientesService = documentosRecientesService;
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
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> documentosRecientesService.registrarVista(u.getId(), MiUnidadItem.TIPO_ESTUDIO, id));
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
        EstudioResponseDTO estudio;
        try {
            estudio = estudioService.obtenerEstudioPorId(id);
        } catch (RuntimeException e) {
            log.warn("Estudio {} no encontrado: {}", id, e.getMessage());
            return "redirect:" + urlRetorno;
        }
        estudioService.incrementarVisualizaciones(id);
        model.addAttribute("estudio", estudio);
        poblarVisor(model, estudio.getArchivoUrl(), estudio.getTitulo());
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
        poblarVisor(model, estudio.getArchivoUrl(), estudio.getTitulo());
        model.addAttribute("urlRetorno", urlRetorno);
        return template;
    }

    private void poblarVisor(Model model, String archivoUrl, String titulo) {
        boolean archivoDisponible = minioStorageService.existeArchivo(archivoUrl);
        model.addAttribute("archivoDisponible", archivoDisponible);
        model.addAttribute("visorUrl", archivoDisponible ? construirVisorUrl(archivoUrl, titulo) : "");
    }

    private String construirVisorUrl(String archivoUrl, String titulo) {
        if (archivoUrl == null || archivoUrl.isBlank()) return "";

        // Extraer objectName de la URL almacenada en BD (independiente del hostname de MinIO)
        String objectName;
        try {
            objectName = minioStorageService.getObjectNameFromUrl(archivoUrl);
        } catch (Exception e) {
            log.warn("No se pudo extraer objectName de URL '{}': {}", archivoUrl, e.getMessage());
            return "";
        }

        // La URL que kkfileview (en Docker) usará para descargar el archivo via el proxy de Spring Boot.
        // Spring Boot hace el fetch a MinIO internamente con localhost:9000, evitando dependencias de red
        // entre kkfileview y MinIO.
        String proxyUrl = kkAccessibleUrl + "/api/documentos/kk-proxy?objectName="
                + URLEncoder.encode(objectName, StandardCharsets.UTF_8);

        String base64Url = URLEncoder.encode(
                Base64.getEncoder().encodeToString(proxyUrl.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
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
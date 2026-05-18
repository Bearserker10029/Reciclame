package org.reciclame.demo.controller;

import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.DocumentosRecientesService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.TematicaService;
import org.reciclame.demo.service.UsuarioService;
import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

@Controller
public class VisualizadorController {

    private final EstudioService estudioService;
    private final NormativaService normativaService;
    private final TematicaService tematicaService;
    private final DocumentosRecientesService documentosRecientesService;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public VisualizadorController(EstudioService estudioService,
                                  NormativaService normativaService,
                                  TematicaService tematicaService,
                                  DocumentosRecientesService documentosRecientesService,
                                  UsuarioService usuarioService,
                                  NotificacionService notificacionService,
                                  AuditoriaService auditoriaService) {
        this.estudioService = estudioService;
        this.normativaService = normativaService;
        this.tematicaService = tematicaService;
        this.documentosRecientesService = documentosRecientesService;
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    private void setupVisualizadorAttributes(Model model, UserDetails user) {
        model.addAttribute("rol", "VISUALIZADOR");
        model.addAttribute("rolNombre", "Visualizador");
        model.addAttribute("notifUrl", "/visualizador/notificaciones");
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
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

    @GetMapping({"/visualizador", "/visualizador/inicio"})
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);

        model.addAttribute("normativas", normativaService.listarNormativasPublicas());
        model.addAttribute("estudios", estudioService.listarEstudiosPublicos());

        var tematicas = tematicaService.listarTematicasTodas();
        model.addAttribute("tematicas", tematicas);
        var conteoPorTematica = normativaService.contarPorTematica();
        model.addAttribute("conteoPorTematica", conteoPorTematica);

        List<Map<String, Object>> donutData = tematicas.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("codigo", t.getCodigo());
            item.put("nombre", t.getNombre());
            item.put("colorHex", t.getColorHex());
            item.put("count", conteoPorTematica.getOrDefault(t.getCodigo(), 0L));
            return item;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("donutData", donutData);

        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            List<NotificacionResponseDTO> notifs = notificacionService.listarPorUsuario(u.getId());
            model.addAttribute("notificacionesRecientes", notifs.stream().limit(5).collect(java.util.stream.Collectors.toList()));

            long descargasCount = notifs.stream()
                    .filter(n -> n.getTipo() != null && n.getTipo().contains("DESCARGA"))
                    .count();
            model.addAttribute("descargasCount", descargasCount);

            model.addAttribute("itemsMiUnidad", 0);
        });

        return "Visualizador/inicio";
    }

    @GetMapping("/visualizador/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        model.addAttribute("normativas", normativaService.listarNormativasPublicas());
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                model.addAttribute("ultimosAccesos", documentosRecientesService.listarUltimos(u.getId(), 4));
            });
        } catch (Exception e) {
            model.addAttribute("ultimosAccesos", java.util.Collections.emptyList());
        }
        return "Visualizador/normativo";
    }

    @GetMapping("/visualizador/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        model.addAttribute("estudios", estudioService.listarEstudiosPublicos());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                model.addAttribute("ultimosAccesos", documentosRecientesService.listarUltimos(u.getId(), 4));
            });
        } catch (Exception e) {
            model.addAttribute("ultimosAccesos", java.util.Collections.emptyList());
        }
        return "Visualizador/estudios";
    }

    @GetMapping("/visualizador/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
        });
        return "Visualizador/notificaciones";
    }

    @PostMapping("/visualizador/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/visualizador/notificaciones";
    }

    @GetMapping("/visualizador/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupVisualizadorAttributes(model, user);
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("usuario", u);

            if (u.getCreatedAt() != null) {
                java.time.LocalDate ld = u.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                model.addAttribute("ultimoAccesoFormatted", ld.toString());
            } else {
                model.addAttribute("ultimoAccesoFormatted", "No registrado");
            }

            var auditoriasPage = auditoriaService.listarAuditoriasPorUsuarioPaginado(u.getId(), 0, 5);
            model.addAttribute("actividadReciente", auditoriasPage.getContent());

            long busquedasCount = auditoriasPage.getContent().stream()
                    .filter(a -> a.getAccion() != null && (a.getAccion().contains("BUSQUEDA") || a.getAccion().contains("CONSULTA")))
                    .count();
            model.addAttribute("busquedasCount", busquedasCount);

            long documentosVistos = auditoriasPage.getContent().stream()
                    .filter(a -> "NORMATIVAS".equals(a.getModulo()) || "ESTUDIOS".equals(a.getModulo()))
                    .count();
            model.addAttribute("documentosVistos", documentosVistos);

            long normativasConsultadas = auditoriasPage.getContent().stream()
                    .filter(a -> "NORMATIVAS".equals(a.getModulo()))
                    .count();
            model.addAttribute("normativasConsultadas", normativasConsultadas);
        });
        return "Visualizador/perfil";
    }

    @PostMapping("/visualizador/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String correoCorporativo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra,
            HttpSession session) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El nombre completo es obligatorio");
            return "redirect:/visualizador/perfil";
        }
        if (correoCorporativo == null || correoCorporativo.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El correo corporativo es obligatorio");
            return "redirect:/visualizador/perfil";
        }
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                if (!u.getId().equals(id)) {
                    ra.addFlashAttribute("errorPerfil", "No tienes permiso para editar este usuario");
                    return;
                }
                var dto = new org.reciclame.demo.dto.UsuarioRequestDTO();
                dto.setNombreCompleto(nombreCompleto);
                dto.setCorreoCorporativo(correoCorporativo);
                var updated = usuarioService.actualizarUsuario(id, dto);
                session.setAttribute("usuarioSesion", new org.reciclame.demo.dto.UsuarioSesion(
                        updated.getId(), updated.getNombreCompleto(), updated.getCorreoCorporativo(),
                        updated.getRolNombre(), null, null));
                ra.addFlashAttribute("exitoPerfil", "Informacion personal actualizada correctamente");
            });
        } catch (Exception e) {
            ra.addFlashAttribute("errorPerfil", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/visualizador/perfil";
    }

    @PostMapping("/visualizador/perfil/cambiar-password")
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorPassword", "Las contrasenias nuevas no coinciden");
            return "redirect:/visualizador/perfil";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("errorPassword", "La contrasenia debe tener al menos 8 caracteres");
            return "redirect:/visualizador/perfil";
        }
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                boolean ok = usuarioService.cambiarPassword(u.getId(), currentPassword, newPassword);
                if (ok) {
                    ra.addFlashAttribute("exitoPassword", "Contrasenia actualizada correctamente");
                } else {
                    ra.addFlashAttribute("errorPassword", "La contrasenia actual es incorrecta");
                }
            });
        } catch (Exception e) {
            ra.addFlashAttribute("errorPassword", "Error al cambiar la contrasenia: " + e.getMessage());
        }
        return "redirect:/visualizador/perfil";
    }
}
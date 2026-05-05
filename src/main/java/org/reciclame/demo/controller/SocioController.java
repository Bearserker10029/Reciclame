package org.reciclame.demo.controller;

import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SocioController {

    private final UsuarioService usuarioService;
    private final EstudioService estudioService;
    private final NormativaService normativaService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;

    public SocioController(UsuarioService usuarioService,
                           EstudioService estudioService,
                           NormativaService normativaService,
                           NotificacionService notificacionService,
                           AuditoriaService auditoriaService) {
        this.usuarioService = usuarioService;
        this.estudioService = estudioService;
        this.normativaService = normativaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        model.addAttribute("notifUrl", "/socio/notificaciones");
        try {
            usuarioService.findByCorreo(correo).ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
        } catch (Exception e) {
            model.addAttribute("usuarioNombre", "Usuario");
        }
    }

    @GetMapping("/socio")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "Socio/inicio";
    }

    @GetMapping("/socio/inicio")
    public String socioInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "Socio/inicio";
    }

    @GetMapping("/socio/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "Socio/notificaciones";
    }

    // ── PERFIL ────────────────────────────────────────────────
    @GetMapping("/socio/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("usuario", u);
            List<?> historialReciente = auditoriaService
                    .listarAuditoriasPorUsuarioPaginado(u.getId(), 0, 5)
                    .getContent();
            model.addAttribute("historialReciente", historialReciente);
        });
        return "Socio/perfil";
    }

    @PostMapping("/socio/perfil/guardar")
    public String guardarPerfil(@RequestParam String nombreCompleto,
                                 @RequestParam(required = false) String cargo,
                                 @AuthenticationPrincipal UserDetails user,
                                 RedirectAttributes ra) {
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                usuarioService.actualizarPerfilPropio(u.getId(), nombreCompleto, cargo));
            ra.addFlashAttribute("okPerfil", "Perfil actualizado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorPerfil", "No se pudo actualizar el perfil: " + e.getMessage());
        }
        return "redirect:/socio/perfil";
    }

    @PostMapping("/socio/perfil/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual,
                                   @RequestParam String nuevoPassword,
                                   @RequestParam String confirmarPassword,
                                   @AuthenticationPrincipal UserDetails user,
                                   RedirectAttributes ra) {
        if (!nuevoPassword.equals(confirmarPassword)) {
            ra.addFlashAttribute("errorPassword", "Las contraseñas nuevas no coinciden.");
            return "redirect:/socio/perfil";
        }
        if (nuevoPassword.length() < 8) {
            ra.addFlashAttribute("errorPassword", "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/socio/perfil";
        }
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                usuarioService.cambiarPassword(u.getId(), passwordActual, nuevoPassword));
            ra.addFlashAttribute("okPassword", "Contraseña actualizada correctamente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorPassword", e.getMessage());
        }
        return "redirect:/socio/perfil";
    }

    // ── NORMATIVO ─────────────────────────────────────────────
    @GetMapping("/socio/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Socio/normativo";
    }

    // ── ESTUDIOS ──────────────────────────────────────────────
    @GetMapping("/socio/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        return "Socio/estudios";
    }

    // ── HISTORIAL ─────────────────────────────────────────────
    @GetMapping("/socio/historial")
    public String historial(Model model, @AuthenticationPrincipal UserDetails user,
                             @RequestParam(defaultValue = "0") int page) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            var pageResult = auditoriaService.listarAuditoriasPorUsuarioPaginado(u.getId(), page, 20);
            model.addAttribute("auditorias", pageResult.getContent());
            model.addAttribute("paginaActual", page);
            model.addAttribute("totalPaginas", pageResult.getTotalPages());
            model.addAttribute("totalElementos", pageResult.getTotalElements());
        });
        return "Socio/historial";
    }

    // ── REPORTES PROPIOS ──────────────────────────────────────
    @GetMapping("/socio/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            var auditorias = auditoriaService.listarAuditoriasPorUsuario(u.getId());

            long totalDescargas = auditorias.stream()
                    .filter(a -> AuditoriaService.ACCION_DESCARGAR_ESTUDIO.equals(a.getAccion())
                              || AuditoriaService.ACCION_DESCARGAR_NORMATIVA.equals(a.getAccion()))
                    .count();
            long descargasEstudios = auditorias.stream()
                    .filter(a -> AuditoriaService.ACCION_DESCARGAR_ESTUDIO.equals(a.getAccion()))
                    .count();
            long descargasNormativas = auditorias.stream()
                    .filter(a -> AuditoriaService.ACCION_DESCARGAR_NORMATIVA.equals(a.getAccion()))
                    .count();
            long totalVisualizaciones = auditorias.stream()
                    .filter(a -> AuditoriaService.ACCION_VER_ESTUDIO.equals(a.getAccion())
                              || AuditoriaService.ACCION_VER_NORMATIVA.equals(a.getAccion()))
                    .count();
            long totalAcciones = auditorias.size();

            // Actividad por módulo para gráfico
            Map<String, Long> porModulo = auditorias.stream()
                    .filter(a -> a.getModulo() != null)
                    .collect(Collectors.groupingBy(a -> a.getModulo(), Collectors.counting()));

            model.addAttribute("totalDescargas", totalDescargas);
            model.addAttribute("descargasEstudios", descargasEstudios);
            model.addAttribute("descargasNormativas", descargasNormativas);
            model.addAttribute("totalVisualizaciones", totalVisualizaciones);
            model.addAttribute("totalAcciones", totalAcciones);
            model.addAttribute("porModulo", porModulo);
            model.addAttribute("ultimasAcciones", auditorias.stream().limit(5).collect(Collectors.toList()));
        });
        return "Socio/reportes";
    }

    // ── NOTIFICACIONES ────────────────────────────────────────
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

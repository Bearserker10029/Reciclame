package org.reciclame.demo.controller;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.dto.SocioReportesView;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.DocumentosRecientesService;
import org.reciclame.demo.service.EmailNotificationService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.reciclame.demo.service.MiUnidadService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.SocioReportesService;
import org.reciclame.demo.service.TematicaService;
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

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class SocioController {

    private final UsuarioService usuarioService;
    private final EstudioService estudioService;
    private final NormativaService normativaService;
    private final NotificacionService notificacionService;
    private final SocioReportesService socioReportesService;
    private final TematicaService tematicaService;
    private final AuditoriaService auditoriaService;
    private final MiUnidadService miUnidadService;
    private final DocumentosRecientesService documentosRecientesService;
    private final ReporteService reporteService;
    private final EmailNotificationService emailNotificationService;

    public SocioController(UsuarioService usuarioService,
                          EstudioService estudioService,
                          NormativaService normativaService,
                          NotificacionService notificacionService,
                          SocioReportesService socioReportesService,
                          TematicaService tematicaService,
                          AuditoriaService auditoriaService,
                          MiUnidadService miUnidadService,
                          DocumentosRecientesService documentosRecientesService,
                          ReporteService reporteService,
                          EmailNotificationService emailNotificationService) {
        this.usuarioService = usuarioService;
        this.estudioService = estudioService;
        this.normativaService = normativaService;
        this.notificacionService = notificacionService;
        this.socioReportesService = socioReportesService;
        this.tematicaService = tematicaService;
        this.auditoriaService = auditoriaService;
        this.miUnidadService = miUnidadService;
        this.documentosRecientesService = documentosRecientesService;
        this.reporteService = reporteService;
        this.emailNotificationService = emailNotificationService;
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
        model.addAttribute("normativas", normativaService.listarNormativasPublicas());
        return "Socio/inicio";
    }

    @GetMapping("/socio/inicio")
    public String socioInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        
        Optional<UsuarioResponseDTO> userOpt = usuarioService.findByCorreo(user.getUsername());
        Long userId = userOpt.map(UsuarioResponseDTO::getId).orElse(null);
        
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
        }).collect(Collectors.toList());
        model.addAttribute("donutData", donutData);
        
        model.addAttribute("topNormativas", normativaService.topMasVistas());

        if (userId != null) {
            List<NotificacionResponseDTO> notifs = notificacionService.listarPorUsuario(userId);
            model.addAttribute("notificacionesRecientes", notifs.stream().limit(5).collect(Collectors.toList()));

            long descargasCount = auditoriaService.listarAuditoriasPorUsuario(userId).stream()
                    .filter(a -> AuditoriaService.ACCION_DESCARGAR_DOCUMENTO.equals(a.getAccion()))
                    .count();
            model.addAttribute("descargasCount", descargasCount);

            model.addAttribute("itemsMiUnidad", miUnidadService.contarItemsUsuario(userId));
            model.addAttribute("recientesUsuario", documentosRecientesService.listarUltimos(userId, 5));
        } else {
            model.addAttribute("notificacionesRecientes", List.of());
            model.addAttribute("descargasCount", 0);
            model.addAttribute("itemsMiUnidad", 0);
            model.addAttribute("recientesUsuario", List.of());
        }

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
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("usuario", u);
            model.addAttribute("totalDocumentos", miUnidadService.contarItemsUsuario(u.getId()));
            model.addAttribute("totalCarpetas", miUnidadService.carpetasConRuta(u.getId()).size());

            if (u.getCreatedAt() != null) {
                java.time.LocalDate ld = u.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                model.addAttribute("ultimoAccesoFormatted", ld.toString());
            } else {
                model.addAttribute("ultimoAccesoFormatted", "No registrado");
            }

            var auditoriasPage = auditoriaService.listarAuditoriasPorUsuarioPaginado(u.getId(), 0, 5);
            model.addAttribute("actividadReciente", auditoriasPage.getContent());

            long descargasCount = auditoriasPage.getContent().stream()
                    .filter(a -> "DESCARGAR_DOCUMENTO".equals(a.getAccion()))
                    .count();
            model.addAttribute("descargasCount", descargasCount);

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
        return "Socio/perfil";
    }

    @GetMapping("/socio/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        // El socio ve TODAS las normativas activas en la tabla.
        // La restricción de visibilidad aplica al abrir el detalle (visor).
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Socio/normativo";
    }

    @GetMapping("/socio/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        // El socio ve TODOS los estudios activos en la tabla.
        // La restricción de visibilidad aplica al abrir el detalle (visor).
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
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

    // ─── Export reportes personales del socio ─────────────────────────
    public static class SocioExportRequest {
        public String tipo;     // "actividad" | "miunidad" | "notificaciones"
        public Integer limite;  // 0 = todos
        public String accion;   // "download" | "email" | "both"
    }

    @PostMapping("/socio/reportes/export")
    @org.springframework.web.bind.annotation.ResponseBody
    public ResponseEntity<?> exportarReporteSocio(
            @RequestBody SocioExportRequest req,
            @AuthenticationPrincipal UserDetails user) {

        var usuarioOpt = usuarioService.findByCorreo(user.getUsername());
        if (usuarioOpt.isEmpty()) return ResponseEntity.badRequest().body("Usuario no encontrado");
        UsuarioResponseDTO uActual = usuarioOpt.get();

        SocioReportesView vista = socioReportesService.construir(uActual.getId());
        String tipo = req.tipo != null ? req.tipo.toLowerCase() : "actividad";
        int limite = req.limite != null ? req.limite : 100;
        String accion = req.accion != null ? req.accion.toLowerCase() : "download";

        String tituloReporte;
        String nombreArchivo;
        String[] headers;
        List<Map<String, Object>> datos = new ArrayList<>();
        int totalRegistros;

        switch (tipo) {
            case "miunidad": {
                tituloReporte = "Mi Unidad";
                nombreArchivo = "mi_unidad_report.xlsx";
                headers = new String[]{"Carpeta", "Título", "Tipo", "Fecha añadido"};
                var filas = vista.getMiUnidad() != null ? vista.getMiUnidad() : List.<org.reciclame.demo.dto.SocioReporteMiUnidadFila>of();
                int max = limite > 0 ? Math.min(limite, filas.size()) : filas.size();
                for (int i = 0; i < max; i++) {
                    var f = filas.get(i);
                    Map<String, Object> row = new HashMap<>();
                    row.put("carpeta", f.getCarpeta());
                    row.put("titulo", f.getTitulo());
                    row.put("tipo", f.getTipoEtiqueta());
                    row.put("fechaAnadido", f.getFechaAnadido());
                    datos.add(row);
                }
                totalRegistros = max;
                break;
            }
            case "notificaciones": {
                tituloReporte = "Mis Notificaciones";
                nombreArchivo = "notificaciones_report.xlsx";
                headers = new String[]{"Fecha", "Título", "Estado"};
                var filas = vista.getNotificaciones() != null ? vista.getNotificaciones() : List.<org.reciclame.demo.dto.SocioReporteNotifFila>of();
                int max = limite > 0 ? Math.min(limite, filas.size()) : filas.size();
                for (int i = 0; i < max; i++) {
                    var f = filas.get(i);
                    Map<String, Object> row = new HashMap<>();
                    row.put("fecha", f.getFecha());
                    row.put("titulo", f.getTitulo());
                    row.put("estado", f.getEstado());
                    datos.add(row);
                }
                totalRegistros = max;
                break;
            }
            default: {
                tituloReporte = "Mi Actividad";
                nombreArchivo = "mi_actividad_report.xlsx";
                headers = new String[]{"Fecha y hora", "Título", "Tipo", "Detalle"};
                var filas = vista.getActividad() != null ? vista.getActividad() : List.<org.reciclame.demo.dto.SocioReporteActividadFila>of();
                int max = limite > 0 ? Math.min(limite, filas.size()) : filas.size();
                for (int i = 0; i < max; i++) {
                    var f = filas.get(i);
                    Map<String, Object> row = new HashMap<>();
                    row.put("fechaHora", f.getFechaHora());
                    row.put("titulo", f.getTitulo());
                    row.put("tipo", f.getTipoEtiqueta());
                    row.put("detalle", f.getDetalle());
                    datos.add(row);
                }
                totalRegistros = max;
                break;
            }
        }

        byte[] archivo = reporteService.generarReporteExcel(tituloReporte,
                "Reporte personal — " + uActual.getNombreCompleto(),
                headers, datos);
        String mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        if ("email".equals(accion) || "both".equals(accion)) {
            emailNotificationService.enviarReporteAdjunto(
                    uActual.getCorreoCorporativo(),
                    uActual.getNombreCompleto(),
                    tituloReporte, totalRegistros,
                    archivo, nombreArchivo, mime);
            if ("email".equals(accion)) {
                Map<String, Object> r = new HashMap<>();
                r.put("enviado", true);
                r.put("totalRegistros", totalRegistros);
                r.put("correo", uActual.getCorreoCorporativo());
                r.put("mensaje", "Reporte enviado a " + uActual.getCorreoCorporativo());
                return ResponseEntity.ok(r);
            }
        }

        return ResponseEntity.ok()
                .header("Content-Type", mime)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .header("X-Registros", String.valueOf(totalRegistros))
                .header("X-Email-Sent", String.valueOf("both".equals(accion)))
                .body(archivo);
    }

    @PostMapping("/socio/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/socio/notificaciones";
    }

    @PostMapping("/socio/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String correoCorporativo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra,
            HttpSession session) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El nombre completo es obligatorio");
            return "redirect:/socio/perfil";
        }
        if (correoCorporativo == null || correoCorporativo.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El correo corporativo es obligatorio");
            return "redirect:/socio/perfil";
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
                ra.addFlashAttribute("exitoPerfil", "Información personal actualizada correctamente");
            });
        } catch (Exception e) {
            ra.addFlashAttribute("errorPerfil", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/socio/perfil";
    }

    @PostMapping("/socio/perfil/cambiar-password")
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorPassword", "Las contraseñas nuevas no coinciden");
            return "redirect:/socio/perfil";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("errorPassword", "La contraseña debe tener al menos 8 caracteres");
            return "redirect:/socio/perfil";
        }
        try {
            usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
                boolean ok = usuarioService.cambiarPassword(u.getId(), currentPassword, newPassword);
                if (ok) {
                    ra.addFlashAttribute("exitoPassword", "Contraseña actualizada correctamente");
                } else {
                    ra.addFlashAttribute("errorPassword", "La contraseña actual es incorrecta");
                }
            });
        } catch (Exception e) {
            ra.addFlashAttribute("errorPassword", "Error al cambiar la contraseña: " + e.getMessage());
        }
        return "redirect:/socio/perfil";
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
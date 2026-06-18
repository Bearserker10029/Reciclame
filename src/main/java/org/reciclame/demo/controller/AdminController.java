package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class AdminController extends BaseReportesController {

    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;
    private final EmpresaService empresaService;
    private final RolService rolService;
    private final TematicaService tematicaService;
    private final EtiquetaService etiquetaService;
    private final TiposNormativaService tiposNormativaService;
    private final EstadosNormativaService estadosNormativaService;
    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final ReporteService reporteService;
    private final ChatbotService chatbotService;

    public AdminController(UsuarioService usuarioService, NormativaService normativaService,
                           EstudioService estudioService, EmpresaService empresaService,
                           RolService rolService, TematicaService tematicaService,
                           EtiquetaService etiquetaService,
                           TiposNormativaService tiposNormativaService,
                           EstadosNormativaService estadosNormativaService,
                           NotificacionService notificacionService, AuditoriaService auditoriaService,
                           ReporteService reporteService, ChatbotService chatbotService) {
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
        this.empresaService = empresaService;
        this.rolService = rolService;
        this.tematicaService = tematicaService;
        this.etiquetaService = etiquetaService;
        this.tiposNormativaService = tiposNormativaService;
        this.estadosNormativaService = estadosNormativaService;
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.reporteService = reporteService;
        this.chatbotService = chatbotService;
    }

    @Override
    protected UsuarioService usuarioService() { return usuarioService; }

    @Override
    protected NormativaService normativaService() { return normativaService; }

    @Override
    protected EstudioService estudioService() { return estudioService; }

    @Override
    protected ReporteService reporteService() { return reporteService; }

    @Override
    protected AuditoriaService auditoriaService() { return auditoriaService; }

    @Override
    protected ChatbotService chatbotService() { return chatbotService; }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
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

    @GetMapping("/admin")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        return adminInicio(model, user);
    }

    @GetMapping("/admin/inicio")
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());

        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);

        // ── Tendencias reales ──
        Instant ahora = Instant.now();
        Instant hoyInicio = ahora.truncatedTo(ChronoUnit.DAYS);
        Instant semanaInicio = ahora.minus(7, ChronoUnit.DAYS);
        Instant mesInicio = ahora.minus(30, ChronoUnit.DAYS);
        Instant mesAnteriorInicio = ahora.minus(60, ChronoUnit.DAYS);

        long usuariosNuevosHoy = usuarioService.contarNuevosDesde(hoyInicio);
        long usuariosNuevosSemana = usuarioService.contarNuevosDesde(semanaInicio);
        long usuariosMesAnterior = usuarioService.contarEntreFechas(mesAnteriorInicio, mesInicio);
        long usuariosEsteMes = usuarioService.contarEntreFechas(mesInicio, ahora);

        long estudiosNuevosSemana = estudioService.contarNuevosDesde(semanaInicio);
        long estudiosMesAnterior = estudioService.contarEntreFechas(mesAnteriorInicio, mesInicio);
        long estudiosEsteMes = estudioService.contarEntreFechas(mesInicio, ahora);

        model.addAttribute("usuariosNuevosHoy", usuariosNuevosHoy);
        model.addAttribute("usuariosNuevosSemana", usuariosNuevosSemana);
        model.addAttribute("pctUsuariosCrecimiento", usuariosMesAnterior > 0
                ? (int) Math.round((usuariosEsteMes - usuariosMesAnterior) * 100.0 / usuariosMesAnterior)
                : 0);

        model.addAttribute("estudiosNuevosSemana", estudiosNuevosSemana);
        model.addAttribute("pctEstudiosCrecimiento", estudiosMesAnterior > 0
                ? (int) Math.round((estudiosEsteMes - estudiosMesAnterior) * 100.0 / estudiosMesAnterior)
                : 0);

        // Descargas mes anterior (aproximado por auditorías)
        long auditoriasMesAnterior = auditoriaService.contarEntreFechas(mesAnteriorInicio, mesInicio);
        long auditoriasEsteMes = auditoriaService.contarEntreFechas(mesInicio, ahora);
        model.addAttribute("pctDescargasCrecimiento", auditoriasMesAnterior > 0
                ? (int) Math.round((auditoriasEsteMes - auditoriasMesAnterior) * 100.0 / auditoriasMesAnterior)
                : 0);

        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 50);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());

        // ── Distribución real por temática para el donut chart ──
        Map<String, Long> byTematica = normativaService.contarPorTematica();
        long normEC   = byTematica.getOrDefault("EC",  0L);
        long normGR   = byTematica.getOrDefault("GR",  0L);
        long normEE   = byTematica.getOrDefault("EE",  0L);
        long normREP  = byTematica.getOrDefault("REP", 0L);
        long totalNorm = byTematica.values().stream().mapToLong(Long::longValue).sum();
        long normOtro  = Math.max(0L, totalNorm - normEC - normGR - normEE - normREP);
        model.addAttribute("normEC",   normEC);
        model.addAttribute("normGR",   normGR);
        model.addAttribute("normEE",   normEE);
        model.addAttribute("normREP",  normREP);
        model.addAttribute("normOtro", normOtro);
        model.addAttribute("totalNormativas", totalNorm);
        model.addAttribute("pctEC",  totalNorm > 0 ? (int) Math.round(normEC  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctGR",  totalNorm > 0 ? (int) Math.round(normGR  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctEE",  totalNorm > 0 ? (int) Math.round(normEE  * 100.0 / totalNorm) : 0);
        model.addAttribute("pctREP", totalNorm > 0 ? (int) Math.round(normREP * 100.0 / totalNorm) : 0);
        model.addAttribute("conteoPorTematica", byTematica);
        var tematicas = tematicaService.listarTematicasTodas();
        List<Map<String, Object>> donutData = tematicas.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("codigo", t.getCodigo());
            item.put("nombre", t.getNombre());
            item.put("colorHex", t.getColorHex());
            item.put("count", byTematica.getOrDefault(t.getCodigo(), 0L));
            return item;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("donutData", donutData);

        return "Admin/inicio";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());
        return "Admin/usuarios";
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
        return "Admin/documentos";
    }

    @GetMapping("/admin/admin-docs")
    public String adminDocs(Model model, @AuthenticationPrincipal UserDetails user) {
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
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstados());
        return "Admin/admin-docs";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 1000);

        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        model.addAttribute("kpiIngresos", (int) auditorias.getTotalElements());
        model.addAttribute("kpiDescargas", totalDescargas);
        model.addAttribute("kpiUsuariosActivos", usuarioService.listarUsuariosActivos().size());
        int totalVisualizaciones = estudios.stream()
                .mapToInt(e -> e.getVisualizacionesCount() != null ? e.getVisualizacionesCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getVisualizacionesCount() != null ? n.getVisualizacionesCount() : 0).sum();
        model.addAttribute("kpiDocsVistos", totalVisualizaciones);

        // Daily activity for the last 30 days using real auditoría data
        Instant hace30d = Instant.now().minus(30, ChronoUnit.DAYS);
        var actividadDiaria = auditoriaService.obtenerActividadDiaria(hace30d, Instant.now());
        List<Integer> chartIngresosData = new ArrayList<>();
        List<String> chartIngresosLabels = new ArrayList<>();
        Map<String, Long> actividadMap = new java.util.LinkedHashMap<>();
        actividadDiaria.forEach(day -> actividadMap.put((String) day.get("fecha"), ((Number) day.get("count")).longValue()));
        java.time.LocalDate diaInicio = java.time.LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            java.time.LocalDate dia = diaInicio.plusDays(i);
            String key = dia.toString();
            chartIngresosLabels.add(String.valueOf(dia.getDayOfMonth()));
            chartIngresosData.add(actividadMap.getOrDefault(key, 0L).intValue());
        }
        model.addAttribute("chartIngresosData", chartIngresosData);
        model.addAttribute("chartIngresosLabels", chartIngresosLabels);
        model.addAttribute("actividadDiaria", actividadDiaria);

        // Real counts by thematic category
        Map<String, Long> byTematicaReportes = normativaService.contarPorTematica();
        List<Integer> chartTematicaData = new ArrayList<>();
        List<String> chartTematicaLabels = Arrays.asList("EC", "GR", "EE", "REP");
        chartTematicaData.add(byTematicaReportes.getOrDefault("EC",  0L).intValue());
        chartTematicaData.add(byTematicaReportes.getOrDefault("GR",  0L).intValue());
        chartTematicaData.add(byTematicaReportes.getOrDefault("EE",  0L).intValue());
        chartTematicaData.add(byTematicaReportes.getOrDefault("REP", 0L).intValue());
        model.addAttribute("chartTematicaData", chartTematicaData);
        model.addAttribute("chartTematicaLabels", chartTematicaLabels);

        List<Map<String, Object>> topDocumentos = new ArrayList<>();
        List<Map<String, Object>> allDocs = new ArrayList<>();
        estudios.forEach(e -> {
            Map<String, Object> doc = new HashMap<>();
            doc.put("titulo", e.getTitulo());
            doc.put("descargas", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            doc.put("tipo", "Estudio");
            allDocs.add(doc);
        });
        normativas.forEach(n -> {
            Map<String, Object> doc = new HashMap<>();
            doc.put("titulo", n.getTitulo());
            doc.put("descargas", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            doc.put("tipo", "Normativa");
            allDocs.add(doc);
        });
        allDocs.sort((a, b) -> ((Integer) b.get("descargas")).compareTo((Integer) a.get("descargas")));
        int maxDescargas = allDocs.isEmpty() ? 1 : ((Integer) allDocs.get(0).get("descargas")).intValue();
        allDocs.stream().limit(5).forEach(doc -> {
            int descargas = ((Integer) doc.get("descargas")).intValue();
            doc.put("porcentaje", (int) (maxDescargas > 0 ? (descargas * 100.0) / maxDescargas : 0));
            topDocumentos.add(doc);
        });
        model.addAttribute("topDocumentos", topDocumentos);

        var todosUsuarios = usuarioService.listarUsuarios();
        Map<Long, Map<String, Object>> usuariosMap = new HashMap<>();
        auditorias.getContent().forEach(a -> {
            Long usuarioId = a.getUsuarioId();
            usuariosMap.computeIfAbsent(usuarioId, k -> {
                Map<String, Object> usr = new HashMap<>();
                usr.put("id", k);
                usr.put("interacciones", 0);
                return usr;
            });
            usuariosMap.get(usuarioId).put("interacciones", (Integer) usuariosMap.get(usuarioId).get("interacciones") + 1);
        });
        todosUsuarios.forEach(u -> {
            if (usuariosMap.containsKey(u.getId())) {
                usuariosMap.get(u.getId()).put("nombre", u.getNombreCompleto());
                usuariosMap.get(u.getId()).put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
                usuariosMap.get(u.getId()).put("rolNombre", u.getRolNombre());
            } else {
                Map<String, Object> usrData = new HashMap<>();
                usrData.put("id", u.getId());
                usrData.put("nombre", u.getNombreCompleto());
                usrData.put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
                usrData.put("rolNombre", u.getRolNombre());
                usrData.put("interacciones", 0);
                usuariosMap.put(u.getId(), usrData);
            }
        });
        List<Map<String, Object>> topUsuarios = new ArrayList<>(usuariosMap.values());
        topUsuarios.sort((a, b) -> ((Integer) b.get("interacciones")).compareTo((Integer) a.get("interacciones")));
        int maxInteracciones = topUsuarios.isEmpty() ? 1 : ((Integer) topUsuarios.get(0).get("interacciones")).intValue();
        List<Map<String, Object>> topUsuariosFinal = new ArrayList<>();
        topUsuarios.stream().filter(usr -> usr.get("nombre") != null && !"SUPERADMIN".equalsIgnoreCase(String.valueOf(usr.get("rolNombre")))).limit(5).forEach(usr -> {
            usr.put("porcentaje", (int) (maxInteracciones > 0 ? ((Integer) usr.get("interacciones") * 100.0) / maxInteracciones : 0));
            topUsuariosFinal.add(usr);
        });
        model.addAttribute("topUsuarios", topUsuariosFinal);

        // Colecciones y valores para JS inline y barras de progreso
        model.addAttribute("normativas", normativas);
        model.addAttribute("estudios", estudios);
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        var usuariosActivosList = usuarioService.listarUsuariosActivos();
        model.addAttribute("usuariosActivos", usuariosActivosList);
        model.addAttribute("maxEstudios", estudios.isEmpty() ? 1 : estudios.size());
        model.addAttribute("maxNormativas", normativas.isEmpty() ? 1 : normativas.size());
        var todosUsuariosR = usuarioService.listarUsuarios();
        int pctUsuariosActivosBar = !todosUsuariosR.isEmpty()
                ? (int)(usuariosActivosList.size() * 100.0 / todosUsuariosR.size()) : 0;
        model.addAttribute("pctUsuariosActivosBar", pctUsuariosActivosBar);
        model.addAttribute("pctDescargasBar", totalDescargas > 0 ? Math.min(100, totalDescargas) : 0);

        // Chatbot Analytics — KPIs por defecto (ultimos 30 dias, sin filtros)
        // Envuelto en try/catch para que un fallo en el modulo del chatbot NO tumbe la pagina de reportes.
        try {
            Instant hace30dChat = Instant.now().minus(30, ChronoUnit.DAYS);
            var chatStats = chatbotService.calcularEstadisticas(hace30dChat, Instant.now());
            model.addAttribute("chatStats", chatStats);
        } catch (Exception ex) {
            System.err.println("[reportes] Error cargando chatStats (chatbot): " + ex.getMessage());
            // KPI vacio para que la UI no se rompa si la BD aun no tiene las tablas/columnas
            model.addAttribute("chatStats", new org.reciclame.demo.dto.ChatbotEstadisticasDTO());
        }
        try {
            model.addAttribute("chatRolList", rolService.listarRoles());
            model.addAttribute("chatUsuariosList", usuarioService.listarUsuarios());
        } catch (Exception ex) {
            System.err.println("[reportes] Error cargando listas de roles/usuarios para filtro chatbot: " + ex.getMessage());
            model.addAttribute("chatRolList", java.util.Collections.emptyList());
            model.addAttribute("chatUsuariosList", java.util.Collections.emptyList());
        }

        return "Admin/reportes";
    }

    @GetMapping("/admin/auditoria/export.csv")
    public void exportarAuditoriaCsv(HttpServletResponse response,
                                      @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria.csv\"");
        var auditorias = auditoriaService.listarAuditoriasUltimas100();
        var pw = response.getWriter();
        pw.println("ID,Fecha,Usuario,Accion,Modulo,Descripcion,Estado,IP");
        for (var a : auditorias) {
            pw.printf("%s,%s,%s,%s,%s,\"%s\",%s,%s%n",
                a.getId(),
                a.getFechaAccion() != null ? a.getFechaAccion().toString().substring(0, 19).replace('T', ' ') : "",
                a.getUsuarioNombre() != null ? a.getUsuarioNombre() : "",
                a.getAccion() != null ? a.getAccion() : "",
                a.getModulo() != null ? a.getModulo() : "",
                a.getDescripcion() != null ? a.getDescripcion().replace("\"", "\"\"") : "",
                a.getEstado() != null ? a.getEstado() : "",
                a.getIpAddress() != null ? a.getIpAddress() : "");
        }
    }

    @GetMapping("/admin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var adminOpt = usuarioService.findByCorreo(user.getUsername());
        var auditoriasPage = adminOpt.map(u -> auditoriaService.listarAuditoriasPorUsuarioId(0, 100, u.getId()))
                .orElse(null);
        model.addAttribute("auditorias", auditoriasPage != null ? auditoriasPage.getContent() : java.util.Collections.emptyList());
        model.addAttribute("totalElements", auditoriasPage != null ? auditoriasPage.getTotalElements() : 0);

        if (auditoriasPage != null && !auditoriasPage.isEmpty()) {
            var conteoPorModulo = auditoriasPage.getContent().stream()
                    .filter(a -> a.getModulo() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            a -> a.getModulo(), java.util.stream.Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> {
                        var m = new java.util.HashMap<String, Object>();
                        m.put("modulo", e.getKey());
                        m.put("count", e.getValue());
                        return m;
                    })
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("conteoPorModulo", conteoPorModulo);
        } else {
            model.addAttribute("conteoPorModulo", java.util.Collections.emptyList());
        }

        // Actividad últimos 7 días
        Instant hace7d = Instant.now().minus(7, ChronoUnit.DAYS);
        var actividadDiaria = auditoriaService.obtenerActividadDiaria(hace7d, Instant.now());
        model.addAttribute("actividadDiaria", actividadDiaria != null ? actividadDiaria : java.util.Collections.emptyList());

        return "Admin/trazabilidad";
    }

    @GetMapping("/admin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
        });
        model.addAttribute("todosUsuarios", usuarioService.listarUsuariosNoSuperadmin());
        return "Admin/notificaciones";
    }

    @PostMapping("/admin/notificaciones/enviar")
    public String enviarNotificacion(
            @RequestParam String destinatario,
            @RequestParam String titulo,
            @RequestParam String mensaje,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if ("todos".equals(destinatario)) {
            int count = notificacionService.enviarNotificacionGlobal(titulo, mensaje, "MENSAJE_ADMIN");
            ra.addFlashAttribute("enviado", "Notificación enviada a " + count + " usuarios.");
        } else {
            try {
                Long id = Long.parseLong(destinatario);
                notificacionService.crearNotificacion(id, titulo, mensaje, "MENSAJE_ADMIN");
                ra.addFlashAttribute("enviado", "Notificación enviada.");
            } catch (NumberFormatException e) {
                ra.addFlashAttribute("error", "Destinatario inválido.");
            }
        }
        return "redirect:/admin/notificaciones";
    }

    @GetMapping("/admin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("usuario", u);

            String nombre = u.getNombreCompleto() != null ? u.getNombreCompleto().trim() : "";
            String[] partes = nombre.split("\\s+");
            String initiales = partes.length >= 2
                    ? String.valueOf(partes[0].charAt(0)) + String.valueOf(partes[1].charAt(0))
                    : (nombre.length() > 0 ? String.valueOf(nombre.charAt(0)) : "?");
            model.addAttribute("initiales", initiales.toUpperCase());

            if (u.getCreatedAt() != null) {
                java.time.LocalDate ld = u.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", new java.util.Locale("es"));
                model.addAttribute("createdAtMes", "Desde " + ld.format(fmt));
                model.addAttribute("createdAtFormatted", ld.toString());
            } else {
                model.addAttribute("createdAtMes", "");
                model.addAttribute("createdAtFormatted", "");
            }

            if (u.getUltimoLogin() != null) {
                java.time.LocalDate ld = u.getUltimoLogin().atZone(java.time.ZoneOffset.UTC).toLocalDate();
                model.addAttribute("ultimoAccesoFormatted", ld.toString());
            } else {
                model.addAttribute("ultimoAccesoFormatted", "No registrado");
            }

            var auditoriasUsuario = auditoriaService.listarAuditoriasPorUsuario(u.getId());

            long descargasCount = auditoriasUsuario.stream()
                    .filter(a -> "DESCARGAR_DOCUMENTO".equals(a.getAccion()))
                    .count();
            model.addAttribute("descargasCount", descargasCount);

            long busquedasCount = auditoriasUsuario.stream()
                    .filter(a -> a.getAccion() != null && (a.getAccion().contains("BUSQUEDA") || a.getAccion().contains("CONSULTA")))
                    .count();
            model.addAttribute("busquedasCount", busquedasCount);

            long documentosVistos = auditoriasUsuario.stream()
                    .filter(a -> "NORMATIVAS".equals(a.getModulo()) || "ESTUDIOS".equals(a.getModulo()))
                    .count();
            model.addAttribute("documentosVistos", documentosVistos);

            long normativasConsultadas = auditoriasUsuario.stream()
                    .filter(a -> "NORMATIVAS".equals(a.getModulo()))
                    .count();
            model.addAttribute("normativasConsultadas", normativasConsultadas);

            var auditoriasRecientes = auditoriaService.listarAuditoriasPorUsuarioPaginado(u.getId(), 0, 5);
            model.addAttribute("actividadReciente", auditoriasRecientes.getContent());
        });
        return "Admin/perfil";
    }

    @PostMapping("/admin/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String correoCorporativo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra,
            HttpSession session) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El nombre completo es obligatorio");
            return "redirect:/admin/perfil";
        }
        if (correoCorporativo == null || correoCorporativo.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El correo corporativo es obligatorio");
            return "redirect:/admin/perfil";
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
        return "redirect:/admin/perfil";
    }

    @PostMapping("/admin/perfil/cambiar-password")
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorPassword", "Las contraseñas nuevas no coinciden");
            return "redirect:/admin/perfil";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("errorPassword", "La contraseña debe tener al menos 8 caracteres");
            return "redirect:/admin/perfil";
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
        return "redirect:/admin/perfil";
    }

    @PostMapping("/admin/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/admin/notificaciones";
    }

    @GetMapping("/admin/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId()))
        );
        model.addAttribute("configuracionUrl", "/admin/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/admin/notificaciones");
        return "configuracion-notificaciones";
    }

    @PostMapping("/admin/notificaciones/configuracion")
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
        return "redirect:/admin/notificaciones/configuracion";
    }

    @GetMapping("/admin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "Admin/normativo";
    }

    @GetMapping("/admin/normativo/crear")
    public String normativoCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",          false);
        model.addAttribute("tematicas",       tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa",  tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa",estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas",       etiquetaService.listarEtiquetas());
        return "Admin/normativo-form";
    }

    @GetMapping("/admin/normativo/editar/{id}")
    public String normativoEditar(@PathVariable Long id, Model model,
                                   @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",          true);
        model.addAttribute("normativa",       normativaService.obtenerNormativaPorId(id));
        model.addAttribute("tematicas",       tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa",  tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa",estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas",       etiquetaService.listarEtiquetas());
        return "Admin/normativo-form";
    }

    @GetMapping("/admin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "Admin/estudios";
    }

    @GetMapping("/admin/estudios/crear")
    public String estudiosCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", false);
        model.addAttribute("tematicas",  tematicaService.listarTematicas());
        model.addAttribute("etiquetas",  etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "Admin/estudios-form";
    }

    @GetMapping("/admin/estudios/editar/{id}")
    public String editarEstudio(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        try {
            model.addAttribute("estudio", estudioService.obtenerEstudioPorId(id));
        } catch (Exception e) {
            model.addAttribute("estudio", null);
        }
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        model.addAttribute("isEdit", true);
        return "Admin/estudios-form";
    }

    /*
                                                w#dUCw*&r   vd#a*qdoMZ
                            IW#mzZ8%}          Z#{>><_<<~{oQ,&X+~~~-_?f%;
                          j&0i!l!!!lfbQ       |a\i}vCOZL\ivb8]+_?{t1}-_ha
                          8Ll!]|xuux[/a       -q>1/trLXnX[n@}<+trvnzLx][Mv
                          Bv!?[(nXJJr1o       1k](zZt)/wwtv@]~\z)dwLhdfU#h
                         dkx]\frttx)jOQ       Qk)(XUuJUUQvqO8/jQ|YXzbbQrvoqaX
                     {mdYjkr}{fx(\nXUwp  {/"dWdz{]|fvLQCXWm ;ocufLmkqLJ|nCwb#88&#v
                   *a~_r(/%B[)rr/cnnchwBat]/zX0O)]11(\\\__vW%%8a?[}\JwOp}cmpL/)ftxO%Wn
                  Uh{1tuYn(x?-{\xYYn\f-!><<?rCZJtxvxzXY\>><<>i!ll+/OZYfcO*X)1(}\1|(/||kM#kOd0
               n*aMon)\\xuvUY]-]]}{iiii!><!rrU(x~{vXXX(_<>ii~++~<>~-[|w#MMMowLCCCJJLQLCx/t(uCb#O
         >Y#888hkaaabJvzXC0Qnvvvvnf~iiiiiiil}c!~<<<<<~~~~i~!!-}{}[_1(10kkhha***oooo***#Q)|t|//x*f
     QU&kv\/jrxxnnnvJ00QQL0OUL0JUYb*bm(i!!i!!>><<<~~~~~~+<~i;~}]]}[}(((Jzct[{uXJ0mwpdbLf)|\\()j@
    qdx))(\frrrjjf|}}(xXUCQLJC#oaao*o*o\!ii>>>>><<<~~<~<~~<i;I>?11[?[{}]1))))1}[+liuz/)->~_~>-?v&
   Ld}[}}{1))1{[-{LwbadmJfcXv#&&&&WWWMa?!>i>>><<<><<<<~~~>>!;;i<<_+++_?_--?]]][]_!<a#bYbM#***##Q~
   Xd1]?????--fXrvOqbhhao*#YXaooh&&&W#q!!!ii>>>>>>>>>>>>>>i!;:I<+~><><~<<>>i><~+_<~#0
    xbhokbdZn(xYnfuLZqdbhao#WrZ&wuW&W*nlll!!!!iiiiiiii>>ii!l/Wo]>iiiiiii!!!!!!!!!!iX#
           JZOJ%WYvcLwppdbaoo_UMhroMLa};llIl!!li>i!!!!!i!!!x8Uo{l!!!!!!iiii!!!lllllrh
               qWJzzJOwqpqpbh0?_(kk-qYiIllllli!!!!!iii!!!l{hk &xl!!i>~+~~~~~~<<!lliZB
               ]WCXXJOwqdbdkJ|mkn)J]Z?;lI!l!!li>i!!i!!!!l!Uo  Wdl!i<~+++__+++~<>>!>ka
                BwXXUmqqpdbb+moab|ZrCIIlll!!l!lliii!!!lII~oC  haIlii>_-+__+<<~+_i!!\ho
                CBYYCmmZpdbbutpp/Oxv1lIIlIl!!!!!l!!!lllIIz%   n&II<>>l>-+>>~>!I!++<?kb
                C%UYLZZmdkbpqdbwpb(Q~;IllllIlI;llllllllI!%z    B(lI<?+i;<]?+~++>lIi>n*;
               (#0UCOO0mpdddddkobbqw0l;:;I;IlI!!lllIIII~bL     Bx!<lI_[_ilI<?[]_+~<><aa
              )baUJQZ00wqdddbkkwqkqbm!>l;;::;IIIlllI;:~p&      ;&Y!>-l;-[[-<!l!~?]]+]8>
             t%OXUCQ0QZwwqqpppmphpd#Z!i>ii>lII;;;lli<>?b%       mC;;;l~!I<+>~_>>illl(8
             oMqZmZmmZOmwwpbpqo*wd#*QiiiiIii><<>><<<>>{h*       aX!!iilI><>><<><<+__~&_
             BwUZwZOLL0Zwppmb*hdoMobm~__-+i+++_-----__)h#ooooooohO\{]>II!!!i>>>l:,l<<8/
            >8LLYzzCmpbk*%@$$@@@@@@@$$@$$@@@@@@@@@@@@@@$$$$$$$$$$$$$$$@w}IiI!i!i!!!>>hb
             8bObdpwZJUk@@$$$$$$$$@$$$$$$$$$$$$$$$$$$$$$$$$$$$@@$$$$$8j>>>l!!!l!!>mW
            O*!>cXCwwwq&@$$$$@&###W@$$$$$$$$$$8#8@$$$$$$$$$$$$$$$BM*@$$$@Xi!!lllll!!!b*
            hZucccuvJOp%@$$$%" "!!_%$$$$@$$$$$c xB$$$$$$$$$$$z^J$#x|8$$$$bnxxxxnxnnun8c
                      d&$$$$) }W$BB$hi.  Ik$$$c I; "Z$^   "Zl. ."C<'M$$$$#0
                      p&$$$$? 1&&- ov {$C .$$$c nBm ~%u:"` rBc X$k! M$$$$#0
                      p&$$$$a  }t" oZ `U[ -$$$c }U< nC -X1 rBX }Uk! M$$$@#0
                      QM$$$$$#wmd#m%@ammmbB$$$WwwmmkB@qmm8mo@8wmw&dwB$$$$*x
                       @@$$$$$$$$@$$$$$$$@$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$@B
                        B%$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$&M
                          Q&$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$*J
    */
}
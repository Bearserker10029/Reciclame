package org.reciclame.demo.controller;

import org.reciclame.demo.dto.EmpresaRequestDTO;
import org.reciclame.demo.dto.ParametrosSistemaRequestDTO;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.ChatbotService;
import org.reciclame.demo.service.EmailNotificationService;
import org.reciclame.demo.service.EmpresaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.EtiquetaService;
import org.reciclame.demo.service.EstadosNormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.ParametrosSistemaService;
import org.reciclame.demo.service.ReporteService;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.reciclame.demo.service.RolService;
import org.reciclame.demo.service.TematicaService;
import org.reciclame.demo.service.TiposNormativaService;
import org.reciclame.demo.service.UsuarioService;
import org.reciclame.demo.util.HttpClientIpResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Controller
public class SuperAdminController extends AdminController {

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
    private final ParametrosSistemaService parametrosSistemaService;
    private final ReporteService reporteService;
    private final ChatbotService chatbotService;
    private final SolicitudesRegistroRepository solicitudesRepo;

    public SuperAdminController(UsuarioService usuarioService,
            NormativaService normativaService,
            EstudioService estudioService,
            EmpresaService empresaService,
            RolService rolService,
            TematicaService tematicaService,
            EtiquetaService etiquetaService,
            TiposNormativaService tiposNormativaService,
            EstadosNormativaService estadosNormativaService,
            NotificacionService notificacionService,
            AuditoriaService auditoriaService,
            ParametrosSistemaService parametrosSistemaService,
            ReporteService reporteService,
            ChatbotService chatbotService,
            SolicitudesRegistroRepository solicitudesRepo,
            EmailNotificationService emailNotificationService) {
        super(usuarioService, normativaService, estudioService, empresaService,
                rolService, tematicaService, etiquetaService,
                tiposNormativaService, estadosNormativaService, notificacionService,
                auditoriaService, reporteService, chatbotService, solicitudesRepo,
                emailNotificationService);
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
        this.parametrosSistemaService = parametrosSistemaService;
        this.reporteService = reporteService;
        this.chatbotService = chatbotService;
        this.solicitudesRepo = solicitudesRepo;
    }

    protected void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        model.addAttribute("notifUrl", "/superadmin/notificaciones");
        try {
            usuarioService.findByCorreo(correo).ifPresent(u -> {
                model.addAttribute("usuario", u);
                model.addAttribute("usuarioNombre", u.getNombreCompleto());
                try {
                    model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
                    model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
                } catch (Exception e) {
                    System.err.println("Error cargando notificaciones: " + e.getMessage());
                    model.addAttribute("notificaciones", java.util.Collections.emptyList());
                    model.addAttribute("noLeidasCount", 0);
                }
            });
        } catch (Exception e) {
            System.err.println("Error en setupCommonAttributes: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("notificaciones", java.util.Collections.emptyList());
            model.addAttribute("noLeidasCount", 0);
        }
    }


    @GetMapping("/superadmin")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        return adminInicio(model, user);
    }


    @GetMapping({ "/superadmin/inicio", "/superadmin/" })
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
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
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuarios());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("solicitudesPendientes", solicitudesRepo.findByEstadoOrderByCreatedAtDesc(EstadoSolicitud.PENDIENTE));
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());
        int totalDescargas = estudioService.listarEstudios().stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativaService.listarNormativas().stream()
                        .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);
        var auditoriasPage = auditoriaService.listarAuditoriasPaginado(0, 5);
        model.addAttribute("auditoriasRecientes", auditoriasPage.getContent());
        return "SuperAdmin/inicio";
    }

    @Override
    
    // Kent,FullStack Developer https://github.com/Bearserker10029
    // Si no se muestra los usuarios, este es el culpable:
    
    @GetMapping("/superadmin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuarios());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("solicitudesPendientes", solicitudesRepo.findByEstadoOrderByCreatedAtDesc(EstadoSolicitud.PENDIENTE));
        model.addAttribute("solicitudesResueltas", solicitudesRepo.findByEstadoNotOrderByCreatedAtDesc(EstadoSolicitud.PENDIENTE));
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());
        return "SuperAdmin/usuarios";
    }

    @Override
    @GetMapping("/superadmin/documentos")
    public String documentos(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var estudios = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                        .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("totalDescargas", totalDescargas);
        return "SuperAdmin/documentos";
    }



    @Override
    @GetMapping("/superadmin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var estudios = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        var tematicas = tematicaService.listarTematicas();
        var usuariosActivos = usuarioService.listarUsuariosActivos();

        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                        .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        List<Map<String, Object>> topDocumentos = new ArrayList<>();
        List<Map<String, Object>> allDocs = new ArrayList<>();
        for (var e : estudios) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("nombre", e.getTitulo());
            doc.put("descargas", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            doc.put("tipo", "Estudio");
            allDocs.add(doc);
        }
        for (var n : normativas) {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("nombre", n.getTitulo());
            doc.put("descargas", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            doc.put("tipo", "Normativa");
            allDocs.add(doc);
        }
        allDocs.sort((a, b) -> Integer.compare((int) b.get("descargas"), (int) a.get("descargas")));
        int topLimit = Math.min(5, allDocs.size());
        topDocumentos = new ArrayList<>(allDocs.subList(0, topLimit));

        Instant hace30Dias = Instant.now().minus(Duration.ofDays(30));
        List<Map<String, Object>> actividadDiaria = auditoriaService.obtenerActividadDiaria(hace30Dias, Instant.now());

        List<Map<String, Object>> topUsuarios = auditoriaService.obtenerTopUsuariosActivos(5);
        for (Map<String, Object> u : topUsuarios) {
            Long uId = (Long) u.get("usuarioId");
            try {
                var uDto = usuarioService.obtenerUsuarioPorId(uId);
                u.put("empresaNombre", uDto.getEmpresaNombre() != null ? uDto.getEmpresaNombre() : "Sin empresa");
            } catch (Exception ex) {
                u.put("empresaNombre", "Sin empresa");
            }
        }

        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("tematicas", tematicas);
        model.addAttribute("usuariosActivos", usuariosActivos);
        model.addAttribute("totalDescargas", totalDescargas);
        model.addAttribute("topDocumentos", topDocumentos);
        model.addAttribute("actividadDiaria", actividadDiaria);
        model.addAttribute("topUsuarios", topUsuarios);

        int maxEstudios = estudios.isEmpty() ? 1 : estudios.size();
        int maxNormativas = normativas.isEmpty() ? 1 : normativas.size();
        model.addAttribute("maxEstudios", maxEstudios);
        model.addAttribute("maxNormativas", maxNormativas);

        var todosUsuariosR = usuarioService.listarUsuarios();
        int pctUsuariosActivosBar = !todosUsuariosR.isEmpty()
                ? (int)(usuariosActivos.size() * 100.0 / todosUsuariosR.size()) : 0;
        model.addAttribute("pctUsuariosActivosBar", pctUsuariosActivosBar);
        model.addAttribute("pctDescargasBar", totalDescargas > 0 ? Math.min(100, totalDescargas) : 0);

        // Chatbot Analytics — KPIs por defecto (ultimos 30 dias)
        // Envuelto en try/catch para que un fallo en el modulo del chatbot NO tumbe la pagina de reportes.
        try {
            Instant hace30dChat = Instant.now().minus(Duration.ofDays(30));
            var chatStats = chatbotService.calcularEstadisticas(hace30dChat, Instant.now());
            model.addAttribute("chatStats", chatStats);
        } catch (Exception ex) {
            System.err.println("[reportes] Error cargando chatStats (chatbot): " + ex.getMessage());
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

        return "SuperAdmin/reportes";
    }

    @Override
    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditoriasPage = auditoriaService.listarAuditoriasParaTrazabilidad(0, 100);
        model.addAttribute("auditorias", auditoriasPage.getContent());
        // totalElements = total real en el sistema (para el stat-chip informativo).
        // El filtro de la tabla, sin embargo, es 100% client-side sobre las filas
        // ya cargadas (ver filtrarTraza() en este mismo template) — nunca busca
        // más allá de esas 100. registrosCargados deja eso explícito en el footer
        // para que el SuperAdmin no crea que está filtrando sobre todo el histórico.
        model.addAttribute("totalElements", auditoriasPage.getTotalElements());
        model.addAttribute("registrosCargados", auditoriasPage.getContent().size());

        // Chips por módulo
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

        // Actividad últimos 7 días
        Instant hace7d = Instant.now().minus(7, ChronoUnit.DAYS);
        var actividadDiaria = auditoriaService.obtenerActividadDiaria(hace7d, Instant.now());
        model.addAttribute("actividadDiaria", actividadDiaria != null ? actividadDiaria : java.util.Collections.emptyList());

        return "SuperAdmin/trazabilidad";
    }

    @Override
    @GetMapping("/superadmin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
        });
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        return "SuperAdmin/notificaciones";
    }

    @PostMapping("/superadmin/notificaciones/enviar")
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
return "redirect:/superadmin/notificaciones";
    }

    

    @Override
    @PostMapping("/superadmin/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> notificacionService.marcarComoLeida(id, u.getId()));
        return "redirect:/superadmin/notificaciones";
    }

    @Override
    @GetMapping("/superadmin/perfil")
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

            List<Auditoria> auditoriasUsuario = auditoriaService.listarAuditoriasPorUsuario(u.getId());

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
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "SuperAdmin/perfil";
    }

    @PostMapping("/superadmin/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String correoCorporativo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra,
            HttpSession session,
            HttpServletRequest request) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El nombre completo es obligatorio");
            return "redirect:/superadmin/perfil";
        }
        if (correoCorporativo == null || correoCorporativo.isBlank()) {
            ra.addFlashAttribute("errorPerfil", "El correo corporativo es obligatorio");
            return "redirect:/superadmin/perfil";
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
                var updated = usuarioService.actualizarUsuario(
                        id, dto,
                        u.getId(),
                        u.getNombreCompleto(),
                        HttpClientIpResolver.resolve(request),
                        request.getHeader("User-Agent"));
                // Si NO hay una impersonación activa, refrescar usuarioSesion con los datos nuevos
                // (caso normal: el SuperAdmin edita su propio perfil).
                // Si SÍ hay impersonación activa, NO pisar usuarioSesion: los datos editados
                // corresponden al impersonado, pero el "usuario real" de la sesión sigue siendo
                // el SuperAdmin; pisar aquí causaría que el topbar se quede con datos del
                // impersonado incluso después de salir de la impersonación.
                if (session.getAttribute("IMPERSONATED_USER") == null) {
                    session.setAttribute("usuarioSesion", new org.reciclame.demo.dto.UsuarioSesion(
                            updated.getId(), updated.getNombreCompleto(), updated.getCorreoCorporativo(),
                            updated.getRolNombre(), null, null));
                }
                ra.addFlashAttribute("exitoPerfil", "Información personal actualizada correctamente");
            });
        } catch (Exception e) {
            ra.addFlashAttribute("errorPerfil", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/superadmin/perfil";
    }

    @PostMapping("/superadmin/perfil/cambiar-password")
    public String cambiarPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorPassword", "Las contraseñas nuevas no coinciden");
            return "redirect:/superadmin/perfil";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("errorPassword", "La contraseña debe tener al menos 8 caracteres");
            return "redirect:/superadmin/perfil";
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
        return "redirect:/superadmin/perfil";
    }

    @Override
    @GetMapping("/superadmin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "SuperAdmin/normativo";
    }

    @Override
    @GetMapping("/superadmin/normativo/crear")
    public String normativoCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", false);
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        return "SuperAdmin/normativo-form";
    }

    @Override
    @GetMapping("/superadmin/normativo/editar/{id}")
    public String normativoEditar(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", true);
        model.addAttribute("normativa", normativaService.obtenerNormativaPorId(id));
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTipos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstados());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        return "SuperAdmin/normativo-form";
    }

    @Override
    @GetMapping("/superadmin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "SuperAdmin/estudios";
    }

    @Override
    @GetMapping("/superadmin/estudios/crear")
    public String estudiosCrear(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", false);
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "SuperAdmin/estudios-form";
    }

    @Override
    @GetMapping("/superadmin/estudios/editar/{id}")
    public String editarEstudio(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", true);
        model.addAttribute("estudio", estudioService.obtenerEstudioPorId(id));
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "SuperAdmin/estudios-form";
    }

    @GetMapping("/superadmin/auditoria")
    public String auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditoriasPage = auditoriaService.listarEventosSeguridad(page, size);
        model.addAttribute("auditorias", auditoriasPage.getContent());
        model.addAttribute("totalElements", auditoriasPage.getTotalElements());

        model.addAttribute("fallidosHoy", auditoriaService.contarIntentosFallidosHoy());
        model.addAttribute("impersonaciones", auditoriaService.contarImpersonaciones());
        model.addAttribute("tasaExito", auditoriaService.obtenerTasaExito24h());
        var alertas = auditoriaService.generarAlertasSeguridad();
        model.addAttribute("alertas", alertas != null ? alertas : java.util.Collections.emptyList());
        var topIPs = auditoriaService.obtenerTopIPsFallidas(5);
        model.addAttribute("topIPsFallidas", topIPs != null ? topIPs : java.util.Collections.emptyList());
        var topUsuariosF = auditoriaService.obtenerTopUsuariosFallidos(5);
        model.addAttribute("topUsuariosFallidos", topUsuariosF != null ? topUsuariosF : java.util.Collections.emptyList());

        return "SuperAdmin/auditoria";
    }

    @Override
    @GetMapping("/superadmin/auditoria/export.csv")
    public void exportarAuditoriaCsv(HttpServletResponse response,
                                      @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria.csv\"");
        // Antes exportaba listarAuditoriasUltimas100() (últimas 100 de TODA la
        // auditoría, sin filtro) mientras la pantalla muestra listarEventosSeguridad
        // (filtrado a acciones de seguridad, paginado). El CSV traía un dataset
        // totalmente distinto al que el SuperAdmin estaba viendo/filtrando. Ahora
        // exporta el mismo filtro de eventos de seguridad, acotado a un límite
        // generoso para no disparar una consulta sin cota.
        var auditorias = auditoriaService.listarEventosSeguridad(0, 5000).getContent();
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

    @GetMapping("/superadmin/impersonar")
    public String impersonar(Model model, @AuthenticationPrincipal UserDetails user,
            HttpSession session) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuarios());
        model.addAttribute("estaImpersonando", session.getAttribute("ORIGINAL_USER") != null);
        if (session.getAttribute("IMPERSONATED_USER") != null) {
            model.addAttribute("usuarioImpersonado", session.getAttribute("IMPERSONATED_USER"));
        }
        return "SuperAdmin/impersonar";
    }

    @PostMapping("/superadmin/impersonar/{id}")
    public String impersonarUsuario(@PathVariable Long id, HttpServletRequest request,
            HttpServletResponse response, HttpSession session, RedirectAttributes redirectAttributes) {
        // Para auditoría necesitamos el nombre REAL del SuperAdmin que
        // está ejecutando la acción. NO usar SecurityContextHolder.getName()
        // porque durante una impersonación anidada (o si el flow recarga
        // el contexto) ese getter devuelve el correo del usuario
        // impersonado. Usamos los atributos inmutables fijados al login.
        Long superAdminId = (Long) session.getAttribute("SUPERADMIN_ORIGINAL_ID");
        String superAdminNombre = (String) session.getAttribute("SUPERADMIN_ORIGINAL_NOMBRE");
        String superAdminCorreo = (String) session.getAttribute("SUPERADMIN_ORIGINAL_CORREO");
        if (superAdminId == null || superAdminNombre == null) {
            // Sesión rota: no debería pasar (solo SUPERADMIN llega aquí), pero
            // caemos al SecurityContext por defensa.
            String fallback = SecurityContextHolder.getContext().getAuthentication().getName();
            superAdminNombre = fallback;
            superAdminCorreo = fallback;
        }

        // ORIGINAL_USER se mantiene con el correo por compatibilidad con
        // el flujo de salirImpersonacion que ya lo usa como lookup key.
        session.setAttribute("ORIGINAL_USER", superAdminCorreo);

        Usuario usuarioTarget = usuarioService.obtenerUsuarioEntityPorId(id);

        session.setAttribute("IMPERSONATED_USER", usuarioTarget.getNombreCompleto());

        // Preservar el usuarioSesion original del SuperAdmin para restaurarlo al salir.
        // Si ya hay una sesión ORIGINAL guardada de una impersonación previa anidada, no la pisamos.
        if (session.getAttribute("ORIGINAL_USUARIO_SESION") == null) {
            Object originalSesion = session.getAttribute("usuarioSesion");
            if (originalSesion != null) {
                session.setAttribute("ORIGINAL_USUARIO_SESION", originalSesion);
            }
        }

        // Setear usuarioSesion con los datos del usuario impersonado para que el topbar
        // (que lee de session.usuarioSesion) muestre nombre/rol correctos durante la impersonación.
        session.setAttribute("usuarioSesion", new org.reciclame.demo.dto.UsuarioSesion(
                usuarioTarget.getId(),
                usuarioTarget.getNombreCompleto(),
                usuarioTarget.getCorreoCorporativo(),
                usuarioTarget.getRol() != null ? usuarioTarget.getRol().getNombre() : "USUARIO",
                usuarioTarget.getCargo(),
                usuarioTarget.getFotoUrl()
        ));

        String rolNombre = usuarioTarget.getRol() != null ? usuarioTarget.getRol().getNombre() : "USUARIO";

        List<GrantedAuthority> combinedAuthorities = new ArrayList<>();
        combinedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + rolNombre));
        combinedAuthorities.add(new SimpleGrantedAuthority("ROLE_IMPERSONATOR"));

        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                usuarioTarget.getCorreoCorporativo(),
                "",
                combinedAuthorities);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, combinedAuthorities);

        authToken.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
        // Spring Security 6 no guarda el contexto automáticamente — hay que persistirlo a la sesión
        // para que las llamadas AJAX posteriores usen el auth del usuario impersonado.
        new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                .saveContext(SecurityContextHolder.getContext(), request, response);

        // Auditoría con la IP real del cliente (no null como antes).
        String ipAddress = org.reciclame.demo.util.HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        auditoriaService.registrarAccion(superAdminId, superAdminNombre,
                AuditoriaService.ACCION_IMPERSONAR, AuditoriaService.MODULO_USUARIOS,
                "Impersonar usuario: " + usuarioTarget.getNombreCompleto() + " (" + usuarioTarget.getCorreoCorporativo()
                        + ")",
                null, null, "EXITOSO", null, ipAddress, userAgent);

        String redirectUrl = switch (rolNombre) {
            case "ADMINISTRADOR" -> "/admin/inicio";
            case "SOCIO" -> "/socio/inicio";
            case "VISUALIZADOR" -> "/visualizador/inicio";
            default -> "/admin/inicio";
        };

        return "redirect:" + redirectUrl;
    }

    @PostMapping("/superadmin/impersonar/salir")
    public String salirImpersonacion(HttpSession session, RedirectAttributes redirectAttributes,
                                     HttpServletRequest request, HttpServletResponse response) {
        // Misma idea que en impersonarUsuario: leer el SuperAdmin real desde
        // los atributos inmutables de sesión, no desde el SecurityContext
        // (que ahora contiene la auth del usuario impersonado).
        Long superAdminId = (Long) session.getAttribute("SUPERADMIN_ORIGINAL_ID");
        String superAdminNombre = (String) session.getAttribute("SUPERADMIN_ORIGINAL_NOMBRE");
        String superAdminCorreo = (String) session.getAttribute("SUPERADMIN_ORIGINAL_CORREO");
        String originalUser = (String) session.getAttribute("ORIGINAL_USER");
        String impersonatedUser = (String) session.getAttribute("IMPERSONATED_USER");

        if (superAdminId == null || superAdminNombre == null) {
            // Defensa: caer al lookup antiguo.
            if (originalUser != null) {
                org.reciclame.demo.model.Usuario u = usuarioService.findByCorreo(originalUser)
                        .map(x -> usuarioService.obtenerUsuarioEntityPorId(x.getId()))
                        .orElse(null);
                if (u != null) {
                    superAdminId = u.getId();
                    superAdminNombre = u.getNombreCompleto();
                }
            }
        }

        // Restaurar el auth del SuperAdmin original en el SecurityContext.
        if (superAdminId != null) {
            org.reciclame.demo.model.Usuario usuarioOriginal = usuarioService.obtenerUsuarioEntityPorId(superAdminId);
            if (usuarioOriginal != null) {
                String rolNombre = usuarioOriginal.getRol() != null ? usuarioOriginal.getRol().getNombre()
                        : "SUPERADMIN";
                Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rolNombre));

                org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                        usuarioOriginal.getCorreoCorporativo(),
                        "",
                        authorities);

                org.springframework.security.authentication.UsernamePasswordAuthenticationToken authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authToken);
                new org.springframework.security.web.context.HttpSessionSecurityContextRepository()
                        .saveContext(SecurityContextHolder.getContext(), request, response);
            }
        }

        // Auditoría con IP real y nombre real del SuperAdmin.
        String ipAddress = org.reciclame.demo.util.HttpClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        if (superAdminId != null && superAdminNombre != null) {
            auditoriaService.registrarAccion(superAdminId, superAdminNombre,
                    AuditoriaService.ACCION_SALIR_IMPERSONACION, AuditoriaService.MODULO_USUARIOS,
                    "Salir de impersonación. Usuario impersonado: " + impersonatedUser,
                    null, null, "EXITOSO", null, ipAddress, userAgent);
        }

        session.removeAttribute("ORIGINAL_USER");
        session.removeAttribute("IMPERSONATED_USER");

        // Restaurar el usuarioSesion del SuperAdmin original (guardado al impersonar).
        // Si por alguna razón ORIGINAL_USUARIO_SESION es null, regenerarlo desde la BD
        // para que el topbar nunca quede con datos del impersonado.
        Object originalSesion = session.getAttribute("ORIGINAL_USUARIO_SESION");
        if (originalSesion != null) {
            session.setAttribute("usuarioSesion", originalSesion);
        } else if (originalUser != null) {
            org.reciclame.demo.model.Usuario usuarioOriginal = usuarioService.findByCorreo(originalUser)
                    .map(u -> usuarioService.obtenerUsuarioEntityPorId(u.getId()))
                    .orElse(null);
            if (usuarioOriginal != null) {
                session.setAttribute("usuarioSesion", new org.reciclame.demo.dto.UsuarioSesion(
                        usuarioOriginal.getId(),
                        usuarioOriginal.getNombreCompleto(),
                        usuarioOriginal.getCorreoCorporativo(),
                        usuarioOriginal.getRol() != null ? usuarioOriginal.getRol().getNombre() : "SUPERADMIN",
                        usuarioOriginal.getCargo(),
                        usuarioOriginal.getFotoUrl()
                ));
            }
        }
        session.removeAttribute("ORIGINAL_USUARIO_SESION");

        redirectAttributes.addFlashAttribute("mensaje", "Has vuelto a tu sesión de SuperAdmin");
        return "redirect:/superadmin/inicio";
    }

    @GetMapping("/superadmin/empresas")
    public String empresas(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        return "SuperAdmin/empresas";
    }

    @GetMapping("/superadmin/empresas/nueva")
    public String nuevaEmpresaForm(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/empresa-form";
    }

    @PostMapping("/superadmin/empresas/guardar")
    public String guardarEmpresa(@Valid @ModelAttribute EmpresaRequestDTO dto,
            BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(e ->
                redirectAttributes.addFlashAttribute("error_" + e.getField(), e.getDefaultMessage()));
            return "redirect:/superadmin/empresas/nueva";
        }
        try {
            empresaService.crearEmpresa(dto);
            redirectAttributes.addFlashAttribute("mensaje", "Empresa creada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/empresas";
    }

    @GetMapping("/superadmin/empresas/editar/{id}")
    public String editarEmpresaForm(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("empresa", empresaService.obtenerEmpresaPorId(id));
        return "SuperAdmin/empresa-form";
    }

    @PostMapping("/superadmin/empresas/editar/{id}")
    public String editarEmpresaGuardar(@PathVariable Long id,
            @Valid @ModelAttribute EmpresaRequestDTO dto,
            BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(e ->
                redirectAttributes.addFlashAttribute("error_" + e.getField(), e.getDefaultMessage()));
            return "redirect:/superadmin/empresas/editar/" + id;
        }
        try {
            empresaService.actualizarEmpresa(id, dto);
            redirectAttributes.addFlashAttribute("mensaje", "Empresa actualizada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/empresas";
    }

    @PostMapping("/superadmin/empresas/eliminar/{id}")
    public String eliminarEmpresa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            empresaService.eliminarEmpresa(id);
            redirectAttributes.addFlashAttribute("mensaje", "Empresa eliminada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/empresas";
    }

    @GetMapping("/superadmin/parametros")
    public String parametros(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("parametros", parametrosSistemaService.listarParametros());
        return "SuperAdmin/parametros";
    }

    @GetMapping("/superadmin/parametros/editar/{id}")
    public String editarParametroForm(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("parametro", parametrosSistemaService.obtenerPorId(id));
        return "SuperAdmin/parametros-form";
    }

    @PostMapping("/superadmin/parametros/editar/{id}")
    public String editarParametroGuardar(@PathVariable Long id,
            @Valid @ModelAttribute ParametrosSistemaRequestDTO dto,
            BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(e ->
                redirectAttributes.addFlashAttribute("error_" + e.getField(), e.getDefaultMessage()));
            return "redirect:/superadmin/parametros/editar/" + id;
        }
        parametrosSistemaService.actualizarParametro(id, dto);
        redirectAttributes.addFlashAttribute("exito", "Parámetro actualizado correctamente.");
        return "redirect:/superadmin/parametros";
    }

    @GetMapping("/superadmin/backups")
    public String backups(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/backups";
    }

    @Override
    @GetMapping("/superadmin/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId())));
        model.addAttribute("configuracionUrl", "/superadmin/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/superadmin/notificaciones");
        return "configuracion-notificaciones";
    }

    @Override
    @PostMapping("/superadmin/notificaciones/configuracion")
    public String guardarConfiguracionNotificaciones(
            @RequestParam(defaultValue = "false") boolean notifNuevasNormativas,
            @RequestParam(defaultValue = "false") boolean notifNuevosEstudios,
            @RequestParam(defaultValue = "false") boolean notifCambiosSeguidos,
            @RequestParam(defaultValue = "false") boolean emailHabilitado,
            @RequestParam(defaultValue = "DIARIA") String frecuenciaEmail,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {
        usuarioService.findByCorreo(user.getUsername())
                .ifPresent(u -> notificacionService.guardarPreferencias(u.getId(), notifNuevasNormativas,
                        notifNuevosEstudios, notifCambiosSeguidos, emailHabilitado, frecuenciaEmail));
        redirectAttributes.addFlashAttribute("guardado", true);
        return "redirect:/superadmin/notificaciones/configuracion";
    }

    @Override
    @GetMapping("/superadmin/admin-docs")
    public String adminDocs(Model model, @AuthenticationPrincipal UserDetails user) {
        return "redirect:/superadmin/documentos";
    }

    @Override
    @GetMapping("/superadmin/reportes/export/auditoria.pdf")
    protected void exportarAuditoriaPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarAuditoriaPDF(response, user);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/auditoria.xlsx")
    protected void exportarAuditoriaXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarAuditoriaXLSX(response, user);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/usuarios.pdf")
    protected void exportarUsuariosPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarUsuariosPDF(response, user);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/usuarios.xlsx")
    protected void exportarUsuariosXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarUsuariosXLSX(response, user);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/normativas.pdf")
    protected void exportarNormativasPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarNormativasPDF(response, user);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/normativas.xlsx")
    protected void exportarNormativasXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        super.exportarNormativasXLSX(response, user);
    }

    // ====================================================================
    //  CHATBOT ANALYTICS — overrides para ruta /superadmin/...
    // ====================================================================

    @Override
    @GetMapping("/superadmin/reportes/export/chatbot.pdf")
    protected void exportarChatbotPDF(jakarta.servlet.http.HttpServletResponse response,
            @RequestParam(value = "desde", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(value = "hasta", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta)
            throws IOException {
        super.exportarChatbotPDF(response, desde, hasta);
    }

    @Override
    @GetMapping("/superadmin/reportes/export/chatbot.xlsx")
    protected void exportarChatbotXLSX(jakarta.servlet.http.HttpServletResponse response,
            @RequestParam(value = "desde", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(value = "hasta", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta)
            throws IOException {
        super.exportarChatbotXLSX(response, desde, hasta);
    }

    // ====================================================================
    //  Endpoints unificados POST — overrides para ruta /superadmin/...
    // ====================================================================

    @Override
    @org.springframework.web.bind.annotation.PostMapping("/superadmin/reportes/export/usuarios")
    @org.springframework.web.bind.annotation.ResponseBody
    protected org.springframework.http.ResponseEntity<?> exportarUsuariosUnificado(
            @org.springframework.web.bind.annotation.RequestBody ExportRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) throws IOException {
        return super.exportarUsuariosUnificado(req, user);
    }

    @Override
    @org.springframework.web.bind.annotation.PostMapping("/superadmin/reportes/export/auditoria")
    @org.springframework.web.bind.annotation.ResponseBody
    protected org.springframework.http.ResponseEntity<?> exportarAuditoriaUnificado(
            @org.springframework.web.bind.annotation.RequestBody ExportRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) {
        return super.exportarAuditoriaUnificado(req, user);
    }

    @Override
    @org.springframework.web.bind.annotation.PostMapping("/superadmin/reportes/export/normativas")
    @org.springframework.web.bind.annotation.ResponseBody
    protected org.springframework.http.ResponseEntity<?> exportarNormativasUnificado(
            @org.springframework.web.bind.annotation.RequestBody ExportRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) {
        return super.exportarNormativasUnificado(req, user);
    }

    @Override
    @org.springframework.web.bind.annotation.PostMapping("/superadmin/reportes/export/chatbot")
    @org.springframework.web.bind.annotation.ResponseBody
    protected org.springframework.http.ResponseEntity<?> exportarChatbotUnificado(
            @org.springframework.web.bind.annotation.RequestBody ExportRequest req,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) {
        return super.exportarChatbotUnificado(req, user);
    }

    // El SuperAdmin sí ve a los usuarios SuperAdmin en sus reportes.
    @Override
    protected boolean excluirSuperadminEnUsuarios() {
        return false;
    }

    // El SuperAdmin también ve las acciones hechas por superadmin en la auditoría.
    @Override
    protected boolean excluirSuperadminEnAuditoria() {
        return false;
    }

    // Solo el SuperAdmin ve la IP de origen en los reportes de auditoría.
    @Override
    protected boolean mostrarIpEnAuditoria() {
        return true;
    }

    // El SuperAdmin también ve las conversaciones/mensajes del chatbot de cualquier usuario,
    // incluidos los superadmin (al hacer pruebas o revisión de calidad).
    @Override
    protected boolean excluirSuperadminEnChatbot() {
        return false;
    }
}
package org.reciclame.demo.controller;

import org.reciclame.demo.dto.EmpresaRequestDTO;
import org.reciclame.demo.dto.ParametrosSistemaRequestDTO;
import org.reciclame.demo.service.AuditoriaService;
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
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.Estudio;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.time.Instant;
import java.time.Duration;

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
            ReporteService reporteService) {
        super(usuarioService, normativaService, estudioService, empresaService,
                rolService, tematicaService, etiquetaService,
                tiposNormativaService, estadosNormativaService, notificacionService, 
                auditoriaService, reporteService);
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

    @Override
    @GetMapping("/superadmin")
    public String inicio(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        model.addAttribute("usuarioNombre", "Super Admin");
        model.addAttribute("noLeidasCount", 0);
        return "SuperAdmin/inicio";
    }

    @Override
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
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
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
    
    /*  ⠀⠀        MEGA KIRBY                  */    @GetMapping("/superadmin/usuarios")
    /*⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⠶⠦⡄⠀⠀⠀⠀⠀⠀⡴⠀⠀⠀     */    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
    /*⠀⢀⣀⠀⠀⠀⣀⠤⠖⠒⠋⡉⠙⢲⣺⢅⡀⠀⠹⡀⠀⠀⠀⢀⡜⠁⠀     */        setupCommonAttributes(model, user.getUsername());
    /*⣼⠉⠀⠉⠓⠏⠁⠀⠀⠀⠀⢯⣧⠈⢿⡆⠈⠓⢴⠇⠀⠀⣠⠊⠀⠀⠀⡀⠀   */        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
    /*⢧⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⠀⡀⠄⠠⢀⠈⢣⡀⠀⠁⠀⢀⡤⠊⠀⠀  */          model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
    /*⠈⢧⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⢀⠎⠀⠀⠀⠘⡇⠀⢧⠀⠐⠊⠁⠀⠀⠀⠀  */          model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
    /*⠀⢸⠳⣄⠀⠀⠀⠀⠀⠀⠀⠈⢺⠀⠀⠀⠀⠀⡇⠀⢸⠀⠀⠀⠀⢀⣀⣀⡀  */          model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
    /*⠀⢸⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⣆⠠⠄⢀⡀⢇⠀⢸⡀⠀⡀⠀⠀⠀⠀⠀  */          model.addAttribute("empresas", empresaService.listarEmpresas());
    /*⠀⠘⡇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢃⠀⠀⠀⠈⠙⠆⡼⠛⢦⡀⠑⠢⣄⠀⠀  */          model.addAttribute("roles", rolService.listarRoles());
    /*⠀⠀⠹⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⡌⠢⣀⠀⢀⡴⡰⠁⠀⢀⡇⠀⠀⠈⠑⠀  */          return "SuperAdmin/usuarios";
    /*⠀⠀⠀⢸⣦⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⡴⠗⠒⠚⠉⠀⠀⠀⠀⠀⠀  */
    /*⠀⠀⠀⡜⠀⠉⠢⢄⣀⠀⠀⠀⠀⠀⣀⡤⠖⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀  */
    /*⠀⠀⠀⡇⠀⠀⠀⠀⣨⠟⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀  */
    /*⠀⠀⠀⠙⠂⠴⠒⠛⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀  */
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

        return "SuperAdmin/reportes";
    }

    @Override
    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditoriasPage = auditoriaService.listarAuditoriasParaTrazabilidad(0, 50);
        model.addAttribute("auditorias", auditoriasPage.getContent());
        model.addAttribute("totalElements", auditoriasPage.getTotalElements());
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

    @Override
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
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/superadmin/notificaciones";
    }

    @Override
    @GetMapping("/superadmin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            List<Auditoria> auditoriasUsuario = auditoriaService.listarAuditoriasPorUsuario(u.getId());

            long descargasCount = auditoriasUsuario.stream()
                    .filter(a -> "DESCARGAR_DOCUMENTO".equals(a.getAccion()))
                    .count();
            model.addAttribute("descargasCount", descargasCount);

            long busquedasCount = auditoriasUsuario.stream()
                    .filter(a -> a.getAccion().contains("BUSQUEDA") || a.getAccion().contains("CONSULTA"))
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

            model.addAttribute("ultimoAcceso", u.getUltimoLogin());

            var auditoriasRecientes = auditoriaService.listarAuditoriasPorUsuarioPaginado(u.getId(), 0, 5);
            model.addAttribute("actividadReciente", auditoriasRecientes.getContent());
        });
        return "SuperAdmin/perfil";
    }

    @PostMapping("/superadmin/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam Long id,
            @RequestParam String nombreCompleto,
            @RequestParam String correoCorporativo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
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
                usuarioService.actualizarUsuario(id, dto);
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
    public String estudiosEditar(@PathVariable Long id, Model model,
            @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit", true);
        model.addAttribute("estudio", estudioService.obtenerEstudioPorId(id));
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativasParaSelector());
        return "SuperAdmin/estudios-form";
    }

    @Override
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

    @GetMapping("/superadmin/impersonar")
    public String impersonar(Model model, @AuthenticationPrincipal UserDetails user,
            HttpSession session) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("estaImpersonando", session.getAttribute("ORIGINAL_USER") != null);
        if (session.getAttribute("IMPERSONATED_USER") != null) {
            model.addAttribute("usuarioImpersonado", session.getAttribute("IMPERSONATED_USER"));
        }
        return "SuperAdmin/impersonar";
    }

    @PostMapping("/superadmin/impersonar/{id}")
    public String impersonarUsuario(@PathVariable Long id, HttpServletRequest request,
            HttpSession session, RedirectAttributes redirectAttributes) {
        String superAdminUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        session.setAttribute("ORIGINAL_USER", superAdminUsername);
        session.setAttribute("IMPERSONATED_USER", usuarioService.obtenerUsuarioPorId(id).getNombreCompleto());

        Usuario usuarioTarget = usuarioService.obtenerUsuarioEntityPorId(id);

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

        auditoriaService.registrarAccion(null, superAdminUsername,
                AuditoriaService.ACCION_IMPERSONAR, AuditoriaService.MODULO_USUARIOS,
                "Impersonar usuario: " + usuarioTarget.getNombreCompleto() + " (" + usuarioTarget.getCorreoCorporativo()
                        + ")",
                null, null, "EXITOSO", null, null, null);

        String redirectUrl = switch (rolNombre) {
            case "ADMINISTRADOR" -> "/admin/inicio";
            case "SOCIO" -> "/socio/inicio";
            case "VISUALIZADOR" -> "/visualizador/inicio";
            default -> "/admin/inicio";
        };

        return "redirect:" + redirectUrl;
    }

    @PostMapping("/superadmin/impersonar/salir")
    public String salirImpersonacion(HttpSession session, RedirectAttributes redirectAttributes) {
        String originalUser = (String) session.getAttribute("ORIGINAL_USER");
        String impersonatedUser = (String) session.getAttribute("IMPERSONATED_USER");

        if (originalUser != null) {
            org.reciclame.demo.model.Usuario usuarioOriginal = usuarioService.findByCorreo(originalUser)
                    .map(u -> usuarioService.obtenerUsuarioEntityPorId(u.getId()))
                    .orElse(null);

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

                auditoriaService.registrarAccion(null, originalUser,
                        AuditoriaService.ACCION_SALIR_IMPERSONACION, AuditoriaService.MODULO_USUARIOS,
                        "Salir de impersonación. Usuario impersonado: " + impersonatedUser,
                        null, null, "EXITOSO", null, null, null);
            }
        }

        session.removeAttribute("ORIGINAL_USER");
        session.removeAttribute("IMPERSONATED_USER");

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

    @GetMapping("/superadmin/diccionarios")
    public String diccionarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("tematicas", tematicaService.listarTematicasTodas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetasTodas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTiposTodos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstadosTodos());
        return "SuperAdmin/diccionarios";
    }

    @GetMapping("/superadmin/legal/cookies")
    public String legalCookies(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "legal/cookies";
    }

    @GetMapping("/superadmin/legal/privacidad")
    public String legalPrivacidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "legal/privacidad";
    }

    @GetMapping("/superadmin/legal/terminos")
    public String legalTerminos(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "legal/terminos";
    }

    @GetMapping("/superadmin/legal/nosotros")
    public String legalNosotros(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "legal/nosotros";
    }

}
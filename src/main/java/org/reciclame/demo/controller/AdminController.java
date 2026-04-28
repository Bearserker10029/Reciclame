package org.reciclame.demo.controller;

import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EmpresaService;
import org.reciclame.demo.service.EstadosNormativaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.EtiquetaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.NormativaService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

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

    public AdminController(UsuarioService usuarioService, NormativaService normativaService,
                           EstudioService estudioService, EmpresaService empresaService,
                           RolService rolService, TematicaService tematicaService,
                           EtiquetaService etiquetaService,
                           TiposNormativaService tiposNormativaService,
                           EstadosNormativaService estadosNormativaService,
                           NotificacionService notificacionService, AuditoriaService auditoriaService) {
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
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("notifUrl", "/admin/notificaciones");
        usuarioService.findByCorreo(correo).ifPresent(u -> model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    @GetMapping("/admin")
    public String inicio(Model model) {
        model.addAttribute("rol", "ADMIN");
        model.addAttribute("rolNombre", "Administrador");
        model.addAttribute("usuarioNombre", "Admin General");
        return "Admin/inicio";
    }

    @GetMapping("/admin/inicio")
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        // Datos para fragmento normativo
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());

        // Datos para fragmento estudios
        model.addAttribute("estudios", estudioService.listarEstudios());

        // Datos para fragmento usuarios
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
        model.addAttribute("todosUsuarios", usuarioService.listarUsuarios());
        model.addAttribute("empresas", empresaService.listarEmpresas());
        model.addAttribute("roles", rolService.listarRoles());

        // Datos para fragmento notificaciones
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });

        // Datos para fragmento documentos
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);

        // Datos para fragmento trazabilidad
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 50);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());

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

    @GetMapping("/admin/auditoria")
    public String auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditorias = auditoriaService.listarAuditoriasPaginado(page, size);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());
        return "Admin/auditoria";
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

    @GetMapping("/admin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());

        // Obtener datos de estudios y normativas
        var estudios   = estudioService.listarEstudios();
        var normativas = normativaService.listarNormativas();
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 1000);

        // ── KPIs ──
        // Total descargas
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();

        // Total de ingresos (registros de auditoría)
        int kpiIngresos = (int) auditorias.getTotalElements();

        // Descargas totales
        int kpiDescargas = totalDescargas;

        // Usuarios activos
        int kpiUsuariosActivos = usuarioService.listarUsuariosActivos().size();

        // Documentos vistos (total downloads desde ambos repositorios)
        int kpiDocsVistos = totalDescargas;

        model.addAttribute("kpiIngresos", kpiIngresos);
        model.addAttribute("kpiDescargas", kpiDescargas);
        model.addAttribute("kpiUsuariosActivos", kpiUsuariosActivos);
        model.addAttribute("kpiDocsVistos", kpiDocsVistos);

        // ── Chart Data: Ingresos (línea temporal) ──
        // Simular 31 días con datos agregados
        List<Integer> chartIngresosData = new ArrayList<>();
        List<String> chartIngresosLabels = new ArrayList<>();
        int auditoriaCount = auditorias.getContent().size();
        int baseValue = auditoriaCount > 0 ? auditoriaCount / 31 : 8;
        for (int i = 1; i <= 31; i++) {
            // Generar valores variados basados en la cantidad de auditorías
            int valor = (int) (baseValue + (Math.random() * baseValue * 0.5));
            chartIngresosData.add(valor);
            chartIngresosLabels.add(String.valueOf(i));
        }

        model.addAttribute("chartIngresosData", chartIngresosData);
        model.addAttribute("chartIngresosLabels", chartIngresosLabels);

        // ── Chart Data: Temáticas (descargas por categoría) ──
        // Distribuir descargas entre 4 categorías
        List<Integer> chartTematicaData = new ArrayList<>();
        List<String> chartTematicaLabels = Arrays.asList("EC", "GR", "EE", "REP");

        int descarPorCategoria = totalDescargas > 0 ? totalDescargas / 4 : 12;
        chartTematicaData.add(descarPorCategoria + 15);      // EC
        chartTematicaData.add(descarPorCategoria + 10);      // GR
        chartTematicaData.add(descarPorCategoria + 5);       // EE
        chartTematicaData.add(descarPorCategoria);           // REP

        model.addAttribute("chartTematicaData", chartTematicaData);
        model.addAttribute("chartTematicaLabels", chartTematicaLabels);

        // ── Top 5 Documentos (por descargas) ──
        List<Map<String, Object>> topDocumentos = new ArrayList<>();

        // Combinar estudios y normativas, ordenar por descargas
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

        // Ordenar por descargas descendente
        allDocs.sort((a, b) -> ((Integer) b.get("descargas")).compareTo((Integer) a.get("descargas")));

        // Calcular porcentaje y tomar top 5
        int maxDescargas = allDocs.isEmpty() ? 1 : ((Integer) allDocs.get(0).get("descargas")).intValue();
        allDocs.stream().limit(5).forEach(doc -> {
            int descargas = ((Integer) doc.get("descargas")).intValue();
            double porcentaje = maxDescargas > 0 ? (descargas * 100.0) / maxDescargas : 0;
            doc.put("porcentaje", (int) porcentaje);
            topDocumentos.add(doc);
        });

        model.addAttribute("topDocumentos", topDocumentos);

        // ── Top 5 Usuarios (por interacciones/auditorías) ──
        var todosUsuarios = usuarioService.listarUsuarios();
        Map<Long, Map<String, Object>> usuariosMap = new HashMap<>();

        // Contar interacciones por usuario desde auditorías
        auditorias.getContent().forEach(a -> {
            Long usuarioId = a.getUsuarioId();
            usuariosMap.computeIfAbsent(usuarioId, k -> {
                Map<String, Object> usr = new HashMap<>();
                usr.put("id", k);
                usr.put("interacciones", 0);
                return usr;
            });
            int count = (Integer) usuariosMap.get(usuarioId).get("interacciones");
            usuariosMap.get(usuarioId).put("interacciones", count + 1);
        });

        // Llenar datos de usuario (nombre, empresa)
        todosUsuarios.forEach(u -> {
            if (usuariosMap.containsKey(u.getId())) {
                Map<String, Object> usrData = usuariosMap.get(u.getId());
                usrData.put("nombre", u.getNombreCompleto());
                usrData.put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
            } else {
                Map<String, Object> usrData = new HashMap<>();
                usrData.put("id", u.getId());
                usrData.put("nombre", u.getNombreCompleto());
                usrData.put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "N/A");
                usrData.put("interacciones", 0);
                usuariosMap.put(u.getId(), usrData);
            }
        });

        // Ordenar por interacciones descendente
        List<Map<String, Object>> topUsuarios = new ArrayList<>(usuariosMap.values());
        topUsuarios.sort((a, b) -> ((Integer) b.get("interacciones")).compareTo((Integer) a.get("interacciones")));

        // Calcular porcentaje y tomar top 5
        int maxInteracciones = topUsuarios.isEmpty() ? 1 : ((Integer) topUsuarios.get(0).get("interacciones")).intValue();
        List<Map<String, Object>> topUsuariosFinal = new ArrayList<>();
        topUsuarios.stream().limit(5).forEach(usr -> {
            int interacciones = (Integer) usr.get("interacciones");
            double porcentaje = maxInteracciones > 0 ? (interacciones * 100.0) / maxInteracciones : 0;
            usr.put("porcentaje", (int) porcentaje);
            topUsuariosFinal.add(usr);
        });

        model.addAttribute("topUsuarios", topUsuariosFinal);

        return "Admin/reportes";
    }

    @GetMapping("/admin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditorias = auditoriaService.listarAuditoriasPaginado(0, 50);
        model.addAttribute("auditorias", auditorias.getContent());
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", auditorias.getTotalPages());
        model.addAttribute("totalElements", auditorias.getTotalElements());
        return "Admin/trazabilidad";
    }

    @GetMapping("/admin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "Admin/notificaciones";
    }

    @GetMapping("/admin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "Admin/perfil";
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
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Admin/estudios-form";
    }

    @GetMapping("/admin/estudios/editar/{id}")
    public String estudiosEditar(@PathVariable Long id, Model model,
                                  @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("isEdit",     true);
        model.addAttribute("estudio",    estudioService.obtenerEstudioPorId(id));
        model.addAttribute("tematicas",  tematicaService.listarTematicas());
        model.addAttribute("etiquetas",  etiquetaService.listarEtiquetas());
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "Admin/estudios-form";
    }
}
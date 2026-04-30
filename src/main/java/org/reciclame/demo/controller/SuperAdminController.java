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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
                tiposNormativaService, estadosNormativaService, notificacionService, auditoriaService);
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
        return "SuperAdmin/inicio";
    }

    @Override
    @GetMapping({ "/superadmin/inicio", "/superadmin/" })
    public String adminInicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        var conteoPorTematica = normativaService.contarPorTematica();
        model.addAttribute("conteoPorTematica", conteoPorTematica);
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
    @GetMapping("/superadmin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        model.addAttribute("usuariosPendientes", usuarioService.listarUsuariosPendientes());
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
        model.addAttribute("estudios", estudios);
        model.addAttribute("normativas", normativas);
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        int totalDescargas = estudios.stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativas.stream()
                        .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);
        model.addAttribute("estudiosCount", estudios.size());
        model.addAttribute("normativasCount", normativas.size());
        return "SuperAdmin/reportes";
    }

    @Override
    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditoriasPage = auditoriaService.listarAuditoriasPaginado(0, 50);
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
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
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

    @GetMapping("/superadmin/auditoria/export.xlsx")
    public void exportarAuditoriaAdminXLSX(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(java.time.ZoneId.of("America/Lima"));
        java.util.List<Auditoria> all = auditoriaService.listarAuditorias();
        List<Map<String, Object>> datos = all.stream().map(a -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("fechaAccion", a.getFechaAccion() != null ? fmt.format(a.getFechaAccion()) : "");
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuarioNombre", a.getUsuarioNombre());
            row.put("ipAddress", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).toList();

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Auditoría",
                "Registro de actividades en la plataforma",
                new String[] { "Fecha", "Acción", "Módulo", "Estado", "Usuario", "IP", "Descripción" }, datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/superadmin/reportes/export/auditoria.pdf")
    public void exportarAuditoriaPDF(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<Auditoria> auditorias = auditoriaService.listarAuditorias();
        List<Map<String, Object>> datos = auditorias.stream().map(a -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("fechaAccion", a.getFechaAccion());
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuarioNombre", a.getUsuarioNombre());
            row.put("ipAddress", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).toList();

        Map<String, Integer> accionesCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (Auditoria a : auditorias) {
            String accionKey = a.getAccion() != null ? a.getAccion() : "SIN_ACCION";
            accionesCount.put(accionKey, accionesCount.getOrDefault(accionKey, 0) + 1);
            String estadoKey = a.getEstado() != null ? a.getEstado() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> accionesData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : accionesCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            accionesData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(accionesData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(estadoData);

        byte[] pdf = reporteService.generarReportePDF("auditoria_report", datos, null, chartData1, chartData2);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/superadmin/reportes/export/auditoria.xlsx")
    public void exportarAuditoriaXLSX(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<Auditoria> auditorias = auditoriaService.listarAuditorias();
        List<Map<String, Object>> datos = auditorias.stream().map(a -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("fecha", a.getFechaAccion() != null ? a.getFechaAccion().toString() : "");
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuario", a.getUsuarioNombre());
            row.put("ip", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).toList();

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Auditoría",
                "Registro de actividades en la plataforma",
                new String[] { "Fecha", "Accion", "Modulo", "Estado", "Usuario", "IP", "Descripcion" }, datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/superadmin/reportes/export/usuarios.pdf")
    public void exportarUsuariosPDF(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<org.reciclame.demo.dto.UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rolNombre", u.getRolNombre());
            row.put("empresaNombre", u.getEmpresaNombre());
            row.put("estadoCuenta", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin());
            return row;
        }).toList();

        Map<String, Integer> rolCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (org.reciclame.demo.dto.UsuarioResponseDTO u : usuarios) {
            String rolKey = u.getRolNombre() != null ? u.getRolNombre() : "SIN_ROL";
            rolCount.put(rolKey, rolCount.getOrDefault(rolKey, 0) + 1);
            String estadoKey = u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : rolCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            rolData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(rolData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(estadoData);

        byte[] pdf = reporteService.generarReportePDF("usuarios_report", datos, null, chartData1, chartData2);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/superadmin/reportes/export/usuarios.xlsx")
    public void exportarUsuariosXLSX(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<org.reciclame.demo.dto.UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rol", u.getRolNombre());
            row.put("empresa", u.getEmpresaNombre());
            row.put("estado", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin() != null ? u.getUltimoLogin().toString() : "");
            return row;
        }).toList();

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Usuarios",
                "Listado de usuarios registrados en la plataforma",
                new String[] { "Nombre Completo", "DNI", "Correo Corporativo", "Cargo", "Rol", "Empresa", "Estado",
                        "Último Login" },
                datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/superadmin/reportes/export/normativas.pdf")
    public void exportarNormativasPDF(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<Map<String, Object>> datos = new java.util.ArrayList<>();
        List<org.reciclame.demo.dto.NormativaResponseDTO> normativas = normativaService.listarNormativas();
        List<org.reciclame.demo.dto.EstudioResponseDTO> estudios = estudioService.listarEstudios();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (org.reciclame.demo.dto.NormativaResponseDTO n : normativas) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre());
            row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    n.getCreatedAt() != null ? fmt.format(n.getCreatedAt().atZone(java.time.ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("titulo", e.getTitulo());
            row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", e.getEstadoActivo() != null && e.getEstadoActivo() ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    e.getCreatedAt() != null ? fmt.format(e.getCreatedAt().atZone(java.time.ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        Map<String, Integer> tipoCount = new LinkedHashMap<>();
        tipoCount.put("Normativa", normativas.size());
        tipoCount.put("Estudio", estudios.size());

        Map<String, Integer> tematicaCount = new LinkedHashMap<>();
        for (org.reciclame.demo.dto.NormativaResponseDTO n : normativas) {
            String key = n.getTematicaNombre() != null ? n.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            String key = e.getTematicaNombre() != null ? e.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }

        Map<Integer, Integer> descargasPorAnio = new LinkedHashMap<>();
        for (org.reciclame.demo.dto.NormativaResponseDTO n : normativas) {
            if (n.getAnio() == null) {
                continue;
            }
            int desc = n.getDescargasCount() != null ? n.getDescargasCount() : 0;
            descargasPorAnio.put(n.getAnio(), descargasPorAnio.getOrDefault(n.getAnio(), 0) + desc);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            if (e.getAnio() == null) {
                continue;
            }
            int desc = e.getDescargasCount() != null ? e.getDescargasCount() : 0;
            descargasPorAnio.put(e.getAnio(), descargasPorAnio.getOrDefault(e.getAnio(), 0) + desc);
        }

        List<Map<String, ?>> tipoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tipoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tipoData.add(row);
        }
        List<Map<String, ?>> tematicaData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tematicaCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tematicaData.add(row);
        }
        List<Map<String, ?>> descargasData = new ArrayList<>();
        List<Integer> aniosOrdenados = new ArrayList<>(descargasPorAnio.keySet());
        aniosOrdenados.sort(Integer::compareTo);
        for (Integer anio : aniosOrdenados) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", anio.toString());
            row.put("value", descargasPorAnio.get(anio));
            descargasData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(tipoData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(tematicaData);
        JRMapCollectionDataSource chartData3 = new JRMapCollectionDataSource(descargasData);

        byte[] pdf = reporteService.generarReportePDF("normativas_estudios_report", datos, null, chartData1, chartData2,
                chartData3);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_estudios_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/superadmin/reportes/export/normativas.xlsx")
    public void exportarNormativasXLSX(jakarta.servlet.http.HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws java.io.IOException {
        List<Map<String, Object>> datos = new java.util.ArrayList<>();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (org.reciclame.demo.dto.NormativaResponseDTO n : normativaService.listarNormativas()) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre());
            row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    n.getCreatedAt() != null ? fmt.format(n.getCreatedAt().atZone(java.time.ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudioService.listarEstudios()) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("titulo", e.getTitulo());
            row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", e.getEstadoActivo() != null && e.getEstadoActivo() ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    e.getCreatedAt() != null ? fmt.format(e.getCreatedAt().atZone(java.time.ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Normativas y Estudios",
                "Listado de normativas y estudios registrados",
                new String[] { "Titulo", "Tipo", "Tematica Nombre", "Estado", "Anio", "Descargas Count",
                        "Fecha Creacion" },
                datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_estudios_report.xlsx\"");
        response.getOutputStream().write(xlsx);
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
        return "SuperAdmin/perfil";
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
        model.addAttribute("normativas", normativaService.listarNormativas());
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
        model.addAttribute("normativas", normativaService.listarNormativas());
        return "SuperAdmin/estudios-form";
    }

    @Override
    @GetMapping("/superadmin/auditoria")
    public String auditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        var auditoriasPage = auditoriaService.listarAuditoriasPaginado(page, size);
        model.addAttribute("auditorias", auditoriasPage.getContent());
        model.addAttribute("totalElements", auditoriasPage.getTotalElements());
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
    public String guardarEmpresa(@RequestParam String razonSocial,
            @RequestParam String ruc,
            @RequestParam String sector,
            RedirectAttributes redirectAttributes) {
        try {
            EmpresaRequestDTO dto = new EmpresaRequestDTO();
            dto.setRazonSocial(razonSocial);
            dto.setRuc(ruc);
            dto.setSector(sector);
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
            @RequestParam String razonSocial,
            @RequestParam String sector,
            RedirectAttributes redirectAttributes) {
        try {
            EmpresaRequestDTO dto = new EmpresaRequestDTO();
            dto.setRazonSocial(razonSocial);
            dto.setSector(sector);
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
            @RequestParam String valor,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Boolean estadoActivo,
            RedirectAttributes redirectAttributes) {
        ParametrosSistemaRequestDTO dto = new ParametrosSistemaRequestDTO();
        dto.setValor(valor);
        dto.setDescripcion(descripcion);
        dto.setEstadoActivo(estadoActivo);
        parametrosSistemaService.actualizarParametro(id, dto);
        redirectAttributes.addFlashAttribute("exito", "Paramáetro actualizado correctamente.");
        return "redirect:/superadmin/parametros";
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

    @GetMapping("/superadmin/diccionarios")
    public String diccionarios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("tematicas", tematicaService.listarTematicasTodas());
        model.addAttribute("etiquetas", etiquetaService.listarEtiquetasTodas());
        model.addAttribute("tiposNormativa", tiposNormativaService.listarTiposTodos());
        model.addAttribute("estadosNormativa", estadosNormativaService.listarEstadosTodos());
        return "SuperAdmin/diccionarios";
    }
}
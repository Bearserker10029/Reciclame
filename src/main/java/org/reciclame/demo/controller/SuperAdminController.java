package org.reciclame.demo.controller;

import org.reciclame.demo.dto.ParametrosSistemaRequestDTO;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.EmpresaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.EtiquetaService;
import org.reciclame.demo.service.EstadosNormativaService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.ParametrosSistemaService;
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
            ParametrosSistemaService parametrosSistemaService) {
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
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        int totalDescargas = estudioService.listarEstudios().stream()
                .mapToInt(e -> e.getDescargasCount() != null ? e.getDescargasCount() : 0).sum()
                + normativaService.listarNormativas().stream()
                        .mapToInt(n -> n.getDescargasCount() != null ? n.getDescargasCount() : 0).sum();
        model.addAttribute("totalDescargas", totalDescargas);
        int estudiosCount = estudioService.listarEstudios().size();
        int normativasCount = normativaService.listarNormativas().size();
        model.addAttribute("estudiosCount", estudiosCount);
        model.addAttribute("normativasCount", normativasCount);
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
        return "SuperAdmin/notificaciones";
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
    public String impersonar(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("usuariosActivos", usuarioService.listarUsuariosActivos());
        return "SuperAdmin/impersonar";
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
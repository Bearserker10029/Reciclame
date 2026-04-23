package org.reciclame.demo.controller;

import org.reciclame.demo.service.EmpresaService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NotificacionService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.RolService;
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

@Controller
public class SuperAdminController {

    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final EstudioService estudioService;
    private final EmpresaService empresaService;
    private final RolService rolService;
    private final TematicaService tematicaService;
    private final NotificacionService notificacionService;

    public SuperAdminController(UsuarioService usuarioService,
                                NormativaService normativaService,
                                EstudioService estudioService,
                                EmpresaService empresaService,
                                RolService rolService,
                                TematicaService tematicaService,
                                NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.estudioService = estudioService;
        this.empresaService = empresaService;
        this.rolService = rolService;
        this.tematicaService = tematicaService;
        this.notificacionService = notificacionService;
    }

    private void setupCommonAttributes(Model model, String correo) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("rolNombre", "SuperAdmin");
        model.addAttribute("notifUrl", "/superadmin/notificaciones");
        usuarioService.findByCorreo(correo).ifPresent(u ->
                model.addAttribute("usuarioNombre", u.getNombreCompleto()));
    }

    private void loadSpaData(Model model, String correo) {
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
        usuarioService.findByCorreo(correo).ifPresent(u -> {
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
    }

    @GetMapping("/superadmin")
    public String raiz(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        loadSpaData(model, user.getUsername());
        return "SuperAdmin/inicio";
    }

    @GetMapping("/superadmin/inicio")
    public String inicio(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        loadSpaData(model, user.getUsername());
        return "SuperAdmin/inicio";
    }

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

    @GetMapping("/superadmin/documentos")
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
        return "SuperAdmin/documentos";
    }

    @GetMapping("/superadmin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/reportes";
    }

    @GetMapping("/superadmin/trazabilidad")
    public String trazabilidad(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/trazabilidad";
    }

    @GetMapping("/superadmin/notificaciones")
    public String notificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u -> {
            model.addAttribute("notificaciones", notificacionService.listarPorUsuario(u.getId()));
            model.addAttribute("noLeidasCount", notificacionService.contarNoLeidas(u.getId()));
        });
        return "SuperAdmin/notificaciones";
    }

    @PostMapping("/superadmin/notificaciones/marcar/{id}")
    public String marcarLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/superadmin/notificaciones";
    }

    @GetMapping("/superadmin/perfil")
    public String perfil(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/perfil";
    }

    @GetMapping("/superadmin/normativo")
    public String normativo(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("normativas", normativaService.listarNormativas());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "SuperAdmin/normativo";
    }

    @GetMapping("/superadmin/estudios")
    public String estudios(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        model.addAttribute("estudios", estudioService.listarEstudios());
        model.addAttribute("tematicas", tematicaService.listarTematicas());
        return "SuperAdmin/estudios";
    }

    @GetMapping("/superadmin/auditoria")
    public String auditoria(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/auditoria";
    }

    @GetMapping("/superadmin/impersonar")
    public String impersonar(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        return "SuperAdmin/impersonar";
    }

    @GetMapping("/superadmin/notificaciones/configuracion")
    public String configuracionNotificaciones(Model model, @AuthenticationPrincipal UserDetails user) {
        setupCommonAttributes(model, user.getUsername());
        usuarioService.findByCorreo(user.getUsername()).ifPresent(u ->
                model.addAttribute("prefs", notificacionService.obtenerOCrearPreferencias(u.getId()))
        );
        model.addAttribute("configuracionUrl", "/superadmin/notificaciones/configuracion");
        model.addAttribute("volverUrl", "/superadmin/notificaciones");
        return "configuracion-notificaciones";
    }

    @PostMapping("/superadmin/notificaciones/configuracion")
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
        return "redirect:/superadmin/notificaciones/configuracion";
    }
}

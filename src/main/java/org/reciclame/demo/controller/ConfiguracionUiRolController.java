package org.reciclame.demo.controller;

import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ConfiguracionUiRolController {

    private static final Map<String, String> MODULO_LABELS_SOCIO = Map.of(
            "estudios", "Estudios",
            "normativo", "Repositorio Normativo",
            "mi-unidad", "Mi Unidad",
            "reportes", "Reportes",
            "perfil", "Mi Perfil",
            "notificaciones", "Notificaciones"
    );

    private static final Map<String, String> MODULO_LABELS_VISUALIZADOR = Map.of(
            "estudios", "Estudios",
            "normativo", "Repositorio Normativo",
            "perfil", "Mi Perfil",
            "notificaciones", "Notificaciones"
    );

    private static final Map<String, String> MODULO_LABELS_ADMIN = Map.of(
            "estudios", "Estudios",
            "normativo", "Repositorio Normativo",
            "usuarios", "Usuarios",
            "admin-docs", "Panel de Documentos",
            "trazabilidad", "Trazabilidad",
            "reportes", "Reportes",
            "diccionarios", "Diccionarios",
            "perfil", "Mi Perfil",
            "notificaciones", "Notificaciones"
    );

    private final ConfiguracionUiRolService service;

    public ConfiguracionUiRolController(ConfiguracionUiRolService service) {
        this.service = service;
    }

    // ── Vista superadmin ───────────────────────────────────────────────────

    @GetMapping("/superadmin/configuracion-vistas")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String paginaConfiguracion(Model model) {
        model.addAttribute("rol", "SUPERADMIN");
        model.addAttribute("socioModulos",     service.getModulosSidebar("SOCIO"));
        model.addAttribute("vizModulos",       service.getModulosSidebar("VISUALIZADOR"));
        model.addAttribute("adminModulos",     service.getModulosSidebar("ADMINISTRADOR"));
        model.addAttribute("allModSocio",  ConfiguracionUiRolService.DEFAULT_MODULOS_SOCIO);
        model.addAttribute("allModViz",    ConfiguracionUiRolService.DEFAULT_MODULOS_VISUALIZADOR);
        model.addAttribute("allModAdmin",  ConfiguracionUiRolService.DEFAULT_MODULOS_ADMIN);
        model.addAttribute("moduloLabelsSocio", MODULO_LABELS_SOCIO);
        model.addAttribute("moduloLabelsViz", MODULO_LABELS_VISUALIZADOR);
        model.addAttribute("moduloLabelsAdmin", MODULO_LABELS_ADMIN);

        return "SuperAdmin/configuracion-vistas";
    }

    // ── API REST ───────────────────────────────────────────────────────────

    @GetMapping("/api/ui-config/{rol}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> getConfig(@PathVariable String rol) {
        String rolUpper = rol.toUpperCase();
        return ResponseEntity.ok(Map.of(
                "modulos_sidebar", service.getModulosSidebar(rolUpper)
        ));
    }

    @PostMapping("/api/ui-config/{rol}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseBody
    public ResponseEntity<Void> guardarConfig(
            @PathVariable String rol,
            @RequestBody Map<String, List<String>> body) {
        String rolUpper = rol.toUpperCase();
        if (body.containsKey("modulos_sidebar"))
            service.guardarModulosSidebar(rolUpper, body.get("modulos_sidebar"));
        return ResponseEntity.ok().build();
    }
}

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

    private final ConfiguracionUiRolService service;

    public ConfiguracionUiRolController(ConfiguracionUiRolService service) {
        this.service = service;
    }

    // ── Vista admin ────────────────────────────────────────────────────────

    @GetMapping({"/admin/configuracion-vistas", "/superadmin/configuracion-vistas"})
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    public String paginaConfiguracion(Model model) {
        // Config actual para SOCIO
        model.addAttribute("socioColsNorm",    service.getColumnasNormativas("SOCIO"));
        model.addAttribute("socioColsEst",     service.getColumnasEstudios("SOCIO"));
        model.addAttribute("socioModulos",     service.getModulosSidebar("SOCIO"));

        // Config actual para VISUALIZADOR
        model.addAttribute("vizColsNorm",      service.getColumnasNormativas("VISUALIZADOR"));
        model.addAttribute("vizColsEst",       service.getColumnasEstudios("VISUALIZADOR"));
        model.addAttribute("vizModulos",       service.getModulosSidebar("VISUALIZADOR"));

        // Listas completas de opciones disponibles
        model.addAttribute("allColsNorm",  ConfiguracionUiRolService.DEFAULT_COLS_NORMATIVAS);
        model.addAttribute("allColsEst",   ConfiguracionUiRolService.DEFAULT_COLS_ESTUDIOS);
        model.addAttribute("allModSocio",  ConfiguracionUiRolService.DEFAULT_MODULOS_SOCIO);
        model.addAttribute("allModViz",    ConfiguracionUiRolService.DEFAULT_MODULOS_VISUALIZADOR);

        return "Admin/configuracion-vistas";
    }

    // ── API REST ───────────────────────────────────────────────────────────

    @GetMapping("/api/ui-config/{rol}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> getConfig(@PathVariable String rol) {
        String rolUpper = rol.toUpperCase();
        return ResponseEntity.ok(Map.of(
                "columnas_normativas", service.getColumnasNormativas(rolUpper),
                "columnas_estudios",   service.getColumnasEstudios(rolUpper),
                "modulos_sidebar",     service.getModulosSidebar(rolUpper)
        ));
    }

    @PostMapping("/api/ui-config/{rol}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
    @ResponseBody
    public ResponseEntity<Void> guardarConfig(
            @PathVariable String rol,
            @RequestBody Map<String, List<String>> body) {

        String rolUpper = rol.toUpperCase();

        if (body.containsKey("columnas_normativas"))
            service.guardarColumnasNormativas(rolUpper, body.get("columnas_normativas"));
        if (body.containsKey("columnas_estudios"))
            service.guardarColumnasEstudios(rolUpper, body.get("columnas_estudios"));
        if (body.containsKey("modulos_sidebar"))
            service.guardarModulosSidebar(rolUpper, body.get("modulos_sidebar"));

        return ResponseEntity.ok().build();
    }
}

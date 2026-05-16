package org.reciclame.demo.service;

import org.reciclame.demo.model.ConfiguracionUiRol;
import org.reciclame.demo.repository.ConfiguracionUiRolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class ConfiguracionUiRolService {

    public static final List<String> DEFAULT_MODULOS_SOCIO =
            List.of("estudios", "normativo", "mi-unidad", "reportes", "perfil", "notificaciones");

    public static final List<String> DEFAULT_MODULOS_VISUALIZADOR =
            List.of("estudios", "normativo", "perfil", "notificaciones");

    public static final List<String> DEFAULT_MODULOS_ADMIN =
            List.of("estudios", "normativo", "usuarios", "admin-docs", "trazabilidad", "reportes", "diccionarios", "perfil", "notificaciones");

    private static final String CLAVE_MODULOS = "modulos_sidebar";

    private final ConfiguracionUiRolRepository repo;

    public ConfiguracionUiRolService(ConfiguracionUiRolRepository repo) {
        this.repo = repo;
    }

    public List<String> getModulosSidebar(String rol) {
        List<String> defaultMods;
        if ("VISUALIZADOR".equals(rol)) {
            defaultMods = DEFAULT_MODULOS_VISUALIZADOR;
        } else if ("ADMINISTRADOR".equals(rol)) {
            defaultMods = DEFAULT_MODULOS_ADMIN;
        } else {
            defaultMods = DEFAULT_MODULOS_SOCIO;
        }
        return repo.findByRolAndClave(rol, CLAVE_MODULOS)
                .map(c -> Arrays.asList(c.getValor().split(",")))
                .orElse(defaultMods);
    }

    @Transactional
    public void guardarModulosSidebar(String rol, List<String> modulos) {
        ConfiguracionUiRol cfg = repo.findByRolAndClave(rol, CLAVE_MODULOS)
                .orElse(new ConfiguracionUiRol(rol, CLAVE_MODULOS, ""));
        cfg.setValor(String.join(",", modulos));
        cfg.setActualizadoAt(Instant.now());
        repo.save(cfg);
    }
}
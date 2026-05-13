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

    // ── Columnas por defecto (todas visibles) ──────────────────────────────
    public static final List<String> DEFAULT_COLS_NORMATIVAS =
            List.of("nombre", "codigo", "organismo", "anio", "tipo", "clasif", "estado", "tematica", "acciones");

    public static final List<String> DEFAULT_COLS_ESTUDIOS =
            List.of("documento", "tematica", "objetivo", "anio", "formato", "acciones");

    public static final List<String> DEFAULT_MODULOS_SOCIO =
            List.of("inicio", "estudios", "normativo", "mi-unidad", "reportes", "perfil", "notificaciones");

    public static final List<String> DEFAULT_MODULOS_VISUALIZADOR =
            List.of("estudios", "normativo", "perfil", "notificaciones");

    private static final String CLAVE_COLS_NORMATIVAS = "columnas_normativas";
    private static final String CLAVE_COLS_ESTUDIOS   = "columnas_estudios";
    private static final String CLAVE_MODULOS         = "modulos_sidebar";

    private final ConfiguracionUiRolRepository repo;

    public ConfiguracionUiRolService(ConfiguracionUiRolRepository repo) {
        this.repo = repo;
    }

    // ── Lectura ────────────────────────────────────────────────────────────

    public List<String> getColumnasNormativas(String rol) {
        return get(rol, CLAVE_COLS_NORMATIVAS, DEFAULT_COLS_NORMATIVAS);
    }

    public List<String> getColumnasEstudios(String rol) {
        return get(rol, CLAVE_COLS_ESTUDIOS, DEFAULT_COLS_ESTUDIOS);
    }

    public List<String> getModulosSidebar(String rol) {
        List<String> defaultMods = "VISUALIZADOR".equals(rol)
                ? DEFAULT_MODULOS_VISUALIZADOR : DEFAULT_MODULOS_SOCIO;
        return get(rol, CLAVE_MODULOS, defaultMods);
    }

    private List<String> get(String rol, String clave, List<String> defaults) {
        return repo.findByRolAndClave(rol, clave)
                .map(c -> Arrays.asList(c.getValor().split(",")))
                .orElse(defaults);
    }

    // ── Escritura ─────────────────────────────────────────────────────────

    @Transactional
    public void guardarColumnasNormativas(String rol, List<String> columnas) {
        guardar(rol, CLAVE_COLS_NORMATIVAS, columnas);
    }

    @Transactional
    public void guardarColumnasEstudios(String rol, List<String> columnas) {
        guardar(rol, CLAVE_COLS_ESTUDIOS, columnas);
    }

    @Transactional
    public void guardarModulosSidebar(String rol, List<String> modulos) {
        guardar(rol, CLAVE_MODULOS, modulos);
    }

    private void guardar(String rol, String clave, List<String> valores) {
        ConfiguracionUiRol cfg = repo.findByRolAndClave(rol, clave)
                .orElse(new ConfiguracionUiRol(rol, clave, ""));
        cfg.setValor(String.join(",", valores));
        cfg.setActualizadoAt(Instant.now());
        repo.save(cfg);
    }
}

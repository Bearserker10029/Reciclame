package org.reciclame.demo.service;

import org.reciclame.demo.dto.SearchItemDTO;
import org.reciclame.demo.dto.SearchResponseDTO;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Servicio central de búsqueda global del topbar.
 *
 * <h3>Reglas de visibilidad por rol (origen del usuario en sesión):</h3>
 * <ul>
 *   <li>{@code SOCIO} → ve TODAS las normativas y estudios (públicas y privadas).
 *       NO busca usuarios (no gestiona usuarios).</li>
 *   <li>{@code VISUALIZADOR} → ve SOLO normativas con {@code es_publica = true}
 *       y estudios con {@code es_publico = true}. Sin usuarios.</li>
 *   <li>{@code ADMINISTRADOR} → ve TODAS las normativas y estudios (públicas y privadas),
 *       y usuarios con rol SOCIO, VISUALIZADOR o ADMINISTRADOR.</li>
 *   <li>{@code SUPERADMIN} → ve TODO sin filtro, incluyendo usuarios SUPERADMIN.</li>
 * </ul>
 *
 * <h3>Filtro por configuración de UI (vía {@code configuracion-vistas.html}):</h3>
 * Si el SuperAdmin desactivó un módulo (ej. "normativo") para un rol (ej. ADMINISTRADOR),
 * la búsqueda global NO devuelve resultados de ese módulo para ese rol. Esto evita
 * que el resultado del buscador sea un link al que el {@code ConfiguracionUiFilter}
 * después le va a tirar 403 — la lista queda consistente con la sidebar y con el
 * acceso real.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final int MAX_POR_TIPO = 10;
    private static final int QUERY_MIN_LENGTH = 2;

    /** SUPERADMIN ve todos los tipos. Los demás roles los filtran desde la config de UI. */
    private static final List<String> ADMIN_TIPOS = Collections.unmodifiableList(
            Arrays.asList("NORMATIVA", "ESTUDIO", "USUARIO"));

    /** Mapeo de módulo de config → tipo de resultado de búsqueda. */
    private static final String MOD_NORMATIVA = "normativo";
    private static final String MOD_ESTUDIOS  = "estudios";
    private static final String MOD_USUARIOS  = "usuarios";

    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParametrosSistemaService parametrosService;
    private final ConfiguracionUiRolService uiConfigService;

    public SearchService(NormativaRepository normativaRepository,
                         EstudioRepository estudioRepository,
                         UsuarioRepository usuarioRepository,
                         ParametrosSistemaService parametrosService,
                         ConfiguracionUiRolService uiConfigService) {
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.usuarioRepository = usuarioRepository;
        this.parametrosService = parametrosService;
        this.uiConfigService = uiConfigService;
    }

    public SearchResponseDTO buscar(String queryRaw, String rolEfectivo) {
        long t0 = System.currentTimeMillis();
        String rol = (rolEfectivo == null) ? "" : rolEfectivo.toUpperCase(Locale.ROOT).trim();
        String q = sanitize(queryRaw);

        List<String> tiposPermitidos = tiposPermitidosParaRol(rol);
        List<SearchItemDTO> items = new ArrayList<>();

        if (q.length() >= QUERY_MIN_LENGTH) {
            Pageable limit = PageRequest.of(0, MAX_POR_TIPO);

            if (tiposPermitidos.contains("NORMATIVA")) {
                items.addAll(buscarNormativas(q, rol, limit));
            }
            if (tiposPermitidos.contains("ESTUDIO")) {
                items.addAll(buscarEstudios(q, rol, limit));
            }
            if (tiposPermitidos.contains("USUARIO")) {
                items.addAll(buscarUsuarios(q, rol, limit));
            }
        }

        return SearchResponseDTO.builder()
                .query(q)
                .rolEfectivo(rol)
                .tiposPermitidos(tiposPermitidos)
                .resultados(items)
                .tiempoMs(System.currentTimeMillis() - t0)
                .build();
    }

    // ─── Reglas de visibilidad ───────────────────────────────────

    /**
     * Devuelve los tipos de resultado que el rol puede VER en la búsqueda global.
     * Aplica el filtro de {@code ConfiguracionUiRolService} para respetar lo
     * que el SuperAdmin configuró en {@code configuracion-vistas.html}: si un
     * módulo (ej. "normativo") está deshabilitado para el rol, no se incluyen
     * resultados de ese tipo.
     * <p>
     * SUPERADMIN bypasea el filtro de UI (puede ver todo, igual que el resto
     * de la app).
     * </p>
     */
    private List<String> tiposPermitidosParaRol(String rol) {
        if ("SUPERADMIN".equals(rol)) {
            return ADMIN_TIPOS;
        }
        // SOCIO / VISUALIZADOR / ADMINISTRADOR: solo los módulos que el SuperAdmin
        // les dejó habilitados en la config de UI.
        List<String> modulosHabilitados = uiConfigService.getModulosSidebar(rol);
        if (modulosHabilitados == null || modulosHabilitados.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tipos = new ArrayList<>(3);
        if (modulosHabilitados.contains(MOD_NORMATIVA)) tipos.add("NORMATIVA");
        if (modulosHabilitados.contains(MOD_ESTUDIOS))  tipos.add("ESTUDIO");
        if (modulosHabilitados.contains(MOD_USUARIOS))  tipos.add("USUARIO");
        return tipos;
    }

    /**
     * SOCIO ve todo (público y privado). VISUALIZADOR solo público.
     * ADMINISTRADOR y SUPERADMIN también ven todo (lo manejan los repos con buscarTodos).
     */
    private boolean esSoloPublico(String rol) {
        return "VISUALIZADOR".equals(rol);
    }

    // ─── Búsquedas por entidad ────────────────────────────────────

    private List<SearchItemDTO> buscarNormativas(String q, String rol, Pageable limit) {
        List<Normativa> lista = esSoloPublico(rol)
                ? normativaRepository.buscarPublico(q, limit)
                : normativaRepository.buscarTodos(q, limit);

        String urlPrefix = urlNormativaParaRol(rol);

        List<SearchItemDTO> result = new ArrayList<>();
        for (Normativa n : lista) {
            result.add(SearchItemDTO.builder()
                    .tipo("NORMATIVA")
                    .id(n.getId())
                    .titulo(n.getTitulo())
                    .subtitulo(n.getDescripcionBreve())
                    .snippet(makeSnippet(n.getDescripcionBreve(), q))
                    .url(urlPrefix + n.getId() + "/ver")
                    .anio(n.getAnio())
                    .publico(Boolean.TRUE.equals(n.getEsPublica()))
                    .ambito(n.getAmbito())
                    .build());
        }
        return result;
    }

    private List<SearchItemDTO> buscarEstudios(String q, String rol, Pageable limit) {
        List<Estudio> lista = esSoloPublico(rol)
                ? estudioRepository.buscarPublico(q, limit)
                : estudioRepository.buscarTodos(q, limit);

        // Rango máximo de años hacia atrás configurado en parametros_sistema.filtro_max_anios_estudio.
        // 0 o ausente = sin límite.
        int maxAnios = parametrosService.getIntegerOrDefault("filtro_max_anios_estudio", 0);
        if (maxAnios > 0) {
            int anioMinimo = java.time.Year.now().getValue() - maxAnios;
            lista = lista.stream()
                    .filter(e -> e.getAnio() == null || e.getAnio() >= anioMinimo)
                    .toList();
        }

        String urlPrefix = urlEstudioParaRol(rol);

        List<SearchItemDTO> result = new ArrayList<>();
        for (Estudio e : lista) {
            result.add(SearchItemDTO.builder()
                    .tipo("ESTUDIO")
                    .id(e.getId())
                    .titulo(e.getTitulo())
                    .subtitulo(e.getDescripcionBreve())
                    .snippet(makeSnippet(e.getDescripcionBreve(), q))
                    .url(urlPrefix + e.getId() + "/ver")
                    .anio(e.getAnio())
                    .publico(Boolean.TRUE.equals(e.getEsPublico()))
                    .build());
        }
        return result;
    }

    private List<SearchItemDTO> buscarUsuarios(String q, String rol, Pageable limit) {
        List<String> rolesPermitidos;
        if ("SUPERADMIN".equals(rol)) {
            rolesPermitidos = Arrays.asList("SOCIO", "VISUALIZADOR", "ADMINISTRADOR", "SUPERADMIN");
        } else {
            // ADMINISTRADOR
            rolesPermitidos = Arrays.asList("SOCIO", "VISUALIZADOR", "ADMINISTRADOR");
        }

        List<Usuario> lista = usuarioRepository.buscarPorRoles(q, rolesPermitidos, limit);

        List<SearchItemDTO> result = new ArrayList<>();
        for (Usuario u : lista) {
            String subtitulo = (u.getCargo() != null && !u.getCargo().isBlank())
                    ? u.getCargo() : u.getCorreoCorporativo();
            result.add(SearchItemDTO.builder()
                    .tipo("USUARIO")
                    .id(u.getId())
                    .titulo(u.getNombreCompleto())
                    .subtitulo(subtitulo)
                    .snippet(makeSnippet(u.getCorreoCorporativo(), q))
                    .url(urlUsuarioParaRol(rol) + u.getId())
                    .rolNombre(u.getRol() != null ? u.getRol().getNombre() : null)
                    .correo(u.getCorreoCorporativo())
                    .empresa(u.getEmpresa() != null ? u.getEmpresa().getRazonSocial() : null)
                    .build());
        }
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private String urlNormativaParaRol(String rol) {
        if ("ADMINISTRADOR".equals(rol))   return "/admin/normativo/";
        if ("SUPERADMIN".equals(rol))      return "/superadmin/normativo/";
        if ("SOCIO".equals(rol))           return "/socio/normativo/";
        // VISUALIZADOR y default
        return "/visualizador/normativo/";
    }

    private String urlEstudioParaRol(String rol) {
        if ("ADMINISTRADOR".equals(rol))   return "/admin/estudios/";
        if ("SUPERADMIN".equals(rol))      return "/superadmin/estudios/";
        if ("SOCIO".equals(rol))           return "/socio/estudios/";
        // VISUALIZADOR y default
        return "/visualizador/estudios/";
    }

    private String urlUsuarioParaRol(String rol) {
        if ("SUPERADMIN".equals(rol))      return "/superadmin/usuarios?focus=";
        // ADMINISTRADOR y default
        return "/admin/usuarios?focus=";
    }

    private String sanitize(String q) {
        if (q == null) return "";
        // Trim básico y colapsar espacios múltiples
        return q.trim().replaceAll("\\s+", " ");
    }

    private String makeSnippet(String texto, String query) {
        if (texto == null || texto.isBlank()) return null;
        String lower = texto.toLowerCase(Locale.ROOT);
        String qLower = query.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(qLower);
        if (idx < 0) {
            return texto.length() > 120 ? texto.substring(0, 117) + "…" : texto;
        }
        int start = Math.max(0, idx - 40);
        int end   = Math.min(texto.length(), idx + qLower.length() + 80);
        String snippet = texto.substring(start, end);
        if (start > 0)     snippet = "…" + snippet;
        if (end < texto.length()) snippet = snippet + "…";
        return snippet;
    }
}

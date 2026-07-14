package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.SearchItemDTO;
import org.reciclame.demo.dto.SearchResponseDTO;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Vista de búsqueda global ("/buscar?q=...").
 * Renderiza una página dedicada con los resultados agrupados por tipo,
 * usando el rol del usuario en sesión para filtrar.
 */
@Controller
public class SearchViewController {

    private final SearchService searchService;

    public SearchViewController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam(value = "q", required = false) String q,
                         HttpSession session,
                         Model model) {
        String rol = "";
        Object attr = session == null ? null : session.getAttribute("usuarioSesion");
        if (attr instanceof UsuarioSesion us) {
            rol = us.getRolNombre();
            model.addAttribute("usuarioSesion", us);
        }

        String queryLimpio = q == null ? "" : q.trim();
        SearchResponseDTO resp = searchService.buscar(queryLimpio, rol);

        model.addAttribute("query", queryLimpio);
        model.addAttribute("rolEfectivo", resp.getRolEfectivo());
        // El sidebar único compara con 'ADMIN' / 'SOCIO' / 'SUPERADMIN' / 'VISUALIZADOR'.
        // El session trae el nombre completo ('ADMINISTRADOR'), por eso normalizamos aquí.
        model.addAttribute("rol", normalizarRolCorto(resp.getRolEfectivo()));
        model.addAttribute("tiposPermitidos", resp.getTiposPermitidos());
        model.addAttribute("resultados", resp.getResultados());
        model.addAttribute("resultsHtml", renderResultsHtml(resp));
        return "buscar";
    }

    /**
     * Mapea el nombre completo del rol (como viene en sesión y de la BD) al
     * alias corto que espera el sidebar compartido (navbar/sidebar.html) y
     * el fragmento del dock. Si el rol no se reconoce, devuelve "" para que
     * el sidebar/dock se omitan en vez de mostrar opciones incorrectas.
     */
    private String normalizarRolCorto(String rolCompleto) {
        if (rolCompleto == null) return "";
        String r = rolCompleto.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (r) {
            case "ADMINISTRADOR" -> "ADMIN";
            case "SUPERADMIN"   -> "SUPERADMIN";
            case "SOCIO"        -> "SOCIO";
            case "VISUALIZADOR" -> "VISUALIZADOR";
            default             -> "";
        };
    }

    // ─────────────────────────────────────────────────────────
    //  Render HTML server-side (separación de tipos, secciones colapsables)
    // ─────────────────────────────────────────────────────────

    private String renderResultsHtml(SearchResponseDTO resp) {
        var resultados = resp.getResultados();
        if (resultados == null || resultados.isEmpty()) {
            return "<div class=\"empty\"><i class=\"fas fa-frown\"></i>"
                 + "No se encontraron resultados para \"" + esc(resp.getQuery()) + "\".</div>";
        }

        // Agrupar por tipo conservando orden de aparición
        java.util.LinkedHashMap<String, java.util.List<SearchItemDTO>> grouped = new java.util.LinkedHashMap<>();
        for (SearchItemDTO it : resultados) {
            grouped.computeIfAbsent(it.getTipo(), k -> new java.util.ArrayList<>()).add(it);
        }

        StringBuilder sb = new StringBuilder();
        for (var entry : grouped.entrySet()) {
            sb.append("<div class=\"section-title\">")
              .append(iconoPara(entry.getKey()))
              .append("<span>").append(labelPara(entry.getKey())).append("</span>")
              .append("<span class=\"badge-count\">").append(entry.getValue().size()).append("</span>")
              .append("</div>")
              .append("<div class=\"results-grid\">");
            for (SearchItemDTO it : entry.getValue()) {
                sb.append(renderCard(it));
            }
            sb.append("</div>");
        }
        return sb.toString();
    }

    private String renderCard(SearchItemDTO it) {
        String iconCls = switch (it.getTipo()) {
            case "NORMATIVA" -> "icon-normativa";
            case "ESTUDIO"   -> "icon-estudio";
            case "USUARIO"   -> "icon-usuario";
            default          -> "icon-normativa";
        };
        String iconChar = switch (it.getTipo()) {
            case "NORMATIVA" -> "fa-gavel";
            case "ESTUDIO"   -> "fa-flask";
            case "USUARIO"   -> "fa-user";
            default          -> "fa-file";
        };

        StringBuilder tags = new StringBuilder("<div class=\"result-tags\">");
        if (it.getTipo().equals("NORMATIVA") || it.getTipo().equals("ESTUDIO")) {
            tags.append("<span class=\"tag ").append(Boolean.TRUE.equals(it.getPublico()) ? "tag-publico" : "tag-privado")
                .append("\">").append(Boolean.TRUE.equals(it.getPublico()) ? "Público" : "Privado").append("</span>");
            if (it.getAnio() != null) {
                tags.append("<span class=\"tag tag-anio\">").append(it.getAnio()).append("</span>");
            }
            if (it.getAmbito() != null && !it.getAmbito().isBlank()) {
                tags.append("<span class=\"tag tag-anio\">").append(esc(it.getAmbito())).append("</span>");
            }
        }
        if (it.getTipo().equals("USUARIO") && it.getRolNombre() != null) {
            tags.append("<span class=\"tag tag-rol\">").append(esc(it.getRolNombre())).append("</span>");
        }
        tags.append("</div>");

        return "<a class=\"result-card\" href=\"" + esc(it.getUrl()) + "\">"
             + "  <div class=\"result-icon " + iconCls + "\"><i class=\"fas " + iconChar + "\"></i></div>"
             + "  <div class=\"result-body\">"
             + "    <div class=\"result-titulo\">" + esc(it.getTitulo()) + "</div>"
             + (it.getSubtitulo() != null
                  ? "<div class=\"result-subtitulo\">" + esc(it.getSubtitulo()) + "</div>" : "")
             + (it.getSnippet() != null
                  ? "<div class=\"result-snippet\">" + esc(it.getSnippet()) + "</div>" : "")
             + tags
             + "  </div>"
             + "</a>";
    }

    private String iconoPara(String tipo) {
        return switch (tipo) {
            case "NORMATIVA" -> "<i class=\"fas fa-gavel\"></i>";
            case "ESTUDIO"   -> "<i class=\"fas fa-flask\"></i>";
            case "USUARIO"   -> "<i class=\"fas fa-user\"></i>";
            default          -> "<i class=\"fas fa-file\"></i>";
        };
    }

    private String labelPara(String tipo) {
        return switch (tipo) {
            case "NORMATIVA" -> "Normativas";
            case "ESTUDIO"   -> "Estudios";
            case "USUARIO"   -> "Usuarios";
            default          -> tipo;
        };
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

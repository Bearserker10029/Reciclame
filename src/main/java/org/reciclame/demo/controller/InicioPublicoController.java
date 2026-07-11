package org.reciclame.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

/**
 * Sirve la página REAL de reciclar.org en /inicio, con un parche en runtime
 * que agrega el item "Repositorio" al submenu de "Ejes de Gestión +".
 *
 * El link del item Repositorio se construye con un href ABSOLUTO apuntando al
 * backend local (no relativo, para que el <base href="https://reciclame.org">
 * que necesita la página para cargar sus assets no lo desvíe hacia afuera).
 *
 * Si no se puede contactar el sitio público, devuelve una página de fallback.
 */
@Controller
public class InicioPublicoController {

    private static final String REPOSITORIO_PATH = "/service/repositorio/login";
    private static final String REPOSITORIO_LABEL = "Repositorio";

    private static final String PATCH_SCRIPT_TEMPLATE =
            "<script>(function(){try{"
            + "var items=document.querySelectorAll('#menu-menuoficial li.menu-item-has-children > a');"
            + "for(var i=0;i<items.length;i++){"
            + "  if(/Ejes de Gesti[oó]n/.test(items[i].textContent)){"
            + "    var parent=items[i].parentNode;"
            + "    var sub=parent.querySelector('ul.sub-menu');"
            + "    if(!sub)return;"
            + "    if(sub.querySelector('a.repositorio-cta'))return;"
            + "    var li=document.createElement('li');"
            + "    li.className='menu-item menu-item-repositorio';"
            + "    li.style.cssText='border-top:1px solid rgba(255,255,255,.12);margin-top:6px;padding-top:6px;';"
            + "    li.innerHTML='<a class=\"repositorio-cta\" href=\"__REPO_URL__\" data-repo=\"true\" style=\"background:linear-gradient(135deg,#26b0cf,#55b579);color:#fff;font-weight:700;display:flex;align-items:center;gap:6px;padding:10px 18px;\">"
            + "      <i class=\"fas fa-folder-open\"></i>__REPO_LABEL__"
            + "      <span style=\"display:inline-block;margin-left:auto;background:#2ECC71;color:#fff;font-size:9px;font-weight:700;padding:2px 6px;border-radius:8px;\">NUEVO</span>"
            + "    </a>';"
            + "    sub.appendChild(li);"
            + "    return;"
            + "  }"
            + "}}catch(e){console.warn('repositorio-patch',e)}})();</script>";

    @Value("${app.public-site-url:https://reciclame.org/}")
    private String publicSiteUrl;

    @Value("${app.backend.base-url:}")
    private String backendBaseUrlOverride;

    @GetMapping("/inicio")
    public ResponseEntity<byte[]> inicio(HttpServletRequest request) {
        String sourceUrl = publicSiteUrl;
        if (!StringUtils.hasText(sourceUrl)) sourceUrl = "https://reciclame.org/";
        if (!sourceUrl.endsWith("/")) sourceUrl = sourceUrl + "/";

        String backendBase = resolveBackendBaseUrl(request);

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(sourceUrl)
                    .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; ReciclameLocalMirror/1.0)")
                    .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                    .build();

            String html = client.get().retrieve().body(String.class);
            if (html == null || html.isBlank()) {
                return fallback("El sitio público devolvió una respuesta vacía.", backendBase);
            }
            html = patchHtml(html, backendBase);
            byte[] body = html.getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
            headers.setCacheControl("no-store, no-cache, must-revalidate");
            return new ResponseEntity<>(body, headers, HttpStatus.OK);
        } catch (Exception e) {
            return fallback("No se pudo cargar el sitio público: " + e.getMessage(), backendBase);
        }
    }


    private String resolveBackendBaseUrl(HttpServletRequest request) {
        if (StringUtils.hasText(backendBaseUrlOverride)) {
            return stripTrailingSlash(backendBaseUrlOverride);
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);
        boolean isDefaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        if (!isDefaultPort) {
            sb.append(":").append(port);
        }
        return sb.toString();
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    // Iconos del theme original (Consultio + Slider Revolution) que dependen de una
    // fuente de íconos propietaria que reciclame.org no sirve en todos los contextos;
    // sin ella se ven como cuadros vacíos. Ninguno tiene función real aquí (el buscador
    // abre un modal inexistente en esta home, y las flechas navegan un slider de una
    // sola imagen), así que se ocultan en vez de mostrar el glifo roto.
    private static final String ICON_FIX_STYLE =
            "\n<style id=\"reciclame-icon-fix\">"
            + ".header-right-item.h-btn-search,sr7-arrow{display:none!important}"
            + "</style>";

    String patchHtml(String html, String backendBase) {
        // 1) <base> para que los assets relativos carguen desde reciclar.org
        if (!html.contains("<base ")) {
            html = html.replaceFirst("<head>",
                    Matcher.quoteReplacement("<head>")
                            + "\n<base href=\"" + publicSiteUrl + "\">");
        }

        // 1b) Ocultar íconos rotos (buscador y flechas del slider, ver ICON_FIX_STYLE)
        if (!html.contains("reciclame-icon-fix") && html.contains("</head>")) {
            html = html.replaceFirst("</head>",
                    Matcher.quoteReplacement(ICON_FIX_STYLE) + "\n</head>");
        }

        // 2) Script de parche (con URL absoluta del backend)
        String repoUrl = backendBase + REPOSITORIO_PATH;
        String script = PATCH_SCRIPT_TEMPLATE
                .replace("__REPO_URL__", Matcher.quoteReplacement(repoUrl))
                .replace("__REPO_LABEL__", Matcher.quoteReplacement(REPOSITORIO_LABEL));
        if (html.contains("</body>")) {
            html = html.replaceFirst("</body>",
                    Matcher.quoteReplacement(script) + "\n</body>");
        } else {
            html = html + script;
        }
        return html;
    }

    private ResponseEntity<byte[]> fallback(String reason, String backendBase) {
        String repoUrl = backendBase + REPOSITORIO_PATH;
        String html = """
                <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
                <title>Recíclame — Inicio</title>
                <style>body{font-family:system-ui;background:#152e4d;color:#fff;display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0}
                .c{text-align:center;max-width:520px;padding:32px}
                h1{font-size:22px;margin:0 0 14px}a{color:#2ECC71;font-weight:700;text-decoration:none}
                p{color:rgba(255,255,255,.8);line-height:1.6}</style></head><body>
                <div class="c">
                  <h1>Recíclame — Inicio</h1>
                  <p>No se pudo cargar el sitio público (reciclame.org) en este momento.</p>
                  <p><small>Motivo: %s</small></p>
                  <p><a href="%s">Ir al Repositorio →</a></p>
                </div></body></html>
                """.formatted(reason, repoUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
        return new ResponseEntity<>(html.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

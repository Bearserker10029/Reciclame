package org.reciclame.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Proxy reverso autenticado para kkfileview.
 *
 * El browser nunca toca el servidor kkfileview directamente; todas las peticiones
 * del visor (HTML, CSS, JS, imágenes de diapositivas) pasan por este endpoint
 * bajo sesión Spring Security. El puerto 8012 de kkfileview no necesita estar
 * expuesto al navegador.
 *
 * Flujo para PPT/Word/Excel:
 *   iframe → /api/documentos/visor → 302 → /api/documentos/kk-view/onlinePreview?url=...
 *         → proxy → kkfileview:8012 → HTML con rutas relativas
 *         → browser carga assets relativos → /api/documentos/kk-view/pptx/... → proxy → kkfileview
 */
@RestController
@RequestMapping("/api/documentos/kk-view")
public class KkfileviewViewerProxyController {

    /** Prefijo del proxy — debe coincidir con @RequestMapping arriba. */
    static final String PROXY_PREFIX = "/api/documentos/kk-view";

    @Value("${kkfileview.url}")
    private String kkfileviewUrl;

    private final HttpClient httpClient;

    public KkfileviewViewerProxyController(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
            requestUri = requestUri.substring(contextPath.length());
        }

        String kkPath = requestUri.substring(PROXY_PREFIX.length());
        if (kkPath.isEmpty()) kkPath = "/";

        String query = request.getQueryString();
        String targetUrl = kkfileviewUrl + kkPath + (query != null ? "?" + query : "");

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofMinutes(5))
                .GET();

        // Tell kkfileview to generate slide image URLs using the browser's origin + proxy prefix,
        // so the browser can load them directly through this proxy without extra URL rewriting.
        String xBaseUrl = buildBrowserBaseUrl(request);
        if (xBaseUrl != null) {
            reqBuilder.header("X-Base-Url", xBaseUrl);
        }

        HttpResponse<InputStream> upstream;
        try {
            upstream = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
            return;
        }

        int status = upstream.statusCode();
        response.setStatus(status);

        String contentType = upstream.headers().firstValue("content-type").orElse(null);
        if (contentType != null) {
            response.setContentType(contentType);
        }

        boolean isHtml = contentType != null && contentType.contains("text/html");
        boolean isCss  = contentType != null && contentType.contains("text/css");

        try (InputStream is = upstream.body()) {
            if (isHtml || isCss) {
                String charset = extractCharset(contentType);
                String body = new String(is.readAllBytes(), charset);
                body = rewriteAbsolutePaths(body);
                // Re-fijar content-type con charset explícito tras reescritura
                String baseType = contentType.split(";")[0].trim();
                response.setContentType(baseType + ";charset=UTF-8");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                response.setContentLengthLong(bytes.length);
                response.getOutputStream().write(bytes);
            } else {
                long contentLength = upstream.headers().firstValueAsLong("content-length").orElse(-1L);
                if (contentLength > 0) {
                    response.setContentLengthLong(contentLength);
                }
                is.transferTo(response.getOutputStream());
            }
        }
    }

    /**
     * Reescribe rutas absolutas (que empiecen con /) para que pasen por el proxy.
     * Las rutas relativas (sin /) ya resuelven correctamente al estar la página
     * bajo /api/documentos/kk-view/.
     * La única ruta absoluta en los templates es /static/bootstrap/js/html5shiv.js
     * dentro de un comentario condicional IE, ignorado por navegadores modernos.
     */
    private String rewriteAbsolutePaths(String body) {
        // Full URLs with kkfileview's port — generated by kkfileview when it reads the base URL
        // from the incoming proxy request (Host: kkfileview:8012). These appear in the inline
        // JSON of ppt.ftl: "url":"http://kkfileview:8012/img/pptx/..." etc.
        body = body.replaceAll("\"url\":\"https?://[^\"]*:8012/",      "\"url\":\""       + PROXY_PREFIX + "/");
        body = body.replaceAll("\"url\": \"https?://[^\"]*:8012/",     "\"url\": \""      + PROXY_PREFIX + "/");
        body = body.replaceAll("\"thumbUrl\":\"https?://[^\"]*:8012/", "\"thumbUrl\":\""  + PROXY_PREFIX + "/");
        body = body.replaceAll("\"thumbUrl\": \"https?://[^\"]*:8012/","\"thumbUrl\": \"" + PROXY_PREFIX + "/");
        body = body.replaceAll("(href|src|data-src)=\"https?://[^\"]*:8012/", "$1=\""     + PROXY_PREFIX + "/");
        body = body.replaceAll("(href|src|data-src)='https?://[^']*:8012/",   "$1='"      + PROXY_PREFIX + "/");
        // Absolute paths starting with /
        body = body.replace("href=\"/",    "href=\""    + PROXY_PREFIX + "/");
        body = body.replace("href='/",     "href='"     + PROXY_PREFIX + "/");
        body = body.replace("src=\"/",     "src=\""     + PROXY_PREFIX + "/");
        body = body.replace("src='/",      "src='"      + PROXY_PREFIX + "/");
        body = body.replace("data-src=\"/","data-src=\""+ PROXY_PREFIX + "/");
        body = body.replace("data-src='/", "data-src='" + PROXY_PREFIX + "/");
        body = body.replace("action=\"/",  "action=\""  + PROXY_PREFIX + "/");
        // CSS url()
        body = body.replace("url('/",      "url('"      + PROXY_PREFIX + "/");
        body = body.replace("url(\"/",     "url(\""     + PROXY_PREFIX + "/");
        // JSON absolute paths (fallback if kkfileview emits relative-like full paths)
        body = body.replace("\"url\":\"/",       "\"url\":\""       + PROXY_PREFIX + "/");
        body = body.replace("\"url\": \"/",      "\"url\": \""      + PROXY_PREFIX + "/");
        body = body.replace("\"thumbUrl\":\"/",  "\"thumbUrl\":\""  + PROXY_PREFIX + "/");
        body = body.replace("\"thumbUrl\": \"/", "\"thumbUrl\": \"" + PROXY_PREFIX + "/");
        return body;
    }

    private String extractCharset(String contentType) {
        if (contentType == null) return "UTF-8";
        for (String part : contentType.split(";")) {
            String t = part.trim();
            if (t.toLowerCase().startsWith("charset=")) {
                return t.substring("charset=".length()).trim();
            }
        }
        return "UTF-8";
    }

    /**
     * Constructs the X-Base-Url header value for kkfileview.
     * kkfileview's BaseUrlFilter uses this header to build the absolute prefix
     * for slide image URLs (e.g. http://localhost:8080/api/documentos/kk-view).
     * Without it, kkfileview would use its own internal host:port, producing URLs
     * the browser can't reach directly.
     */
    private String buildBrowserBaseUrl(HttpServletRequest request) {
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null || proto.isBlank()) proto = request.getScheme();
        else proto = proto.split(",")[0].trim();

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) host = request.getHeader("Host");
        if (host == null || host.isBlank()) {
            int port = request.getServerPort();
            host = request.getServerName()
                    + (("http".equals(proto) && port == 80) || ("https".equals(proto) && port == 443)
                       ? "" : ":" + port);
        } else {
            host = host.split(",")[0].trim();
        }
        return proto + "://" + host + PROXY_PREFIX;
    }
}
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
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Proxy reverso autenticado para kkfileview.
 *
 * El browser nunca toca el servidor kkfileview directamente; todas las peticiones
 * del visor (HTML, CSS, JS, imágenes de diapositivas) pasan por este endpoint
 * bajo sesión Spring Security. El puerto 8012 de kkfileview no necesita estar
 * expuesto al navegador.
 *
 * <p><b>Optimizaciones para archivos Office pesados:</b></p>
 * <ul>
 *   <li>Buffer grande (256 KB) en transferTo para reducir llamadas al sistema y
 *       aprovechar HTTP/2 multiplexing al copiar el stream upstream → cliente.</li>
 *   <li>Patrones regex pre-compilados para no recompilar en cada rewrite.</li>
 *   <li>Skip del rewrite completo si el body upstream no contiene "kkfileview:8012"
 *       (la mayoría de assets son relativos a partir de la segunda navegación).</li>
 *   <li>Headers {@code Cache-Control} agresivos en assets versionados (CSS/JS/img)
 *       para que el browser los reuse sin volver a pedirlos.</li>
 *   <li>Streaming con Content-Length cuando upstream lo provee, para evitar
 *       chunked encoding y mejorar TTFB.</li>
 * </ul>
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

    /** Buffer de 256 KB para streaming; reduce syscalls vs el default de 8 KB del JDK. */
    private static final int STREAM_BUFFER_BYTES = 256 * 1024;

    /** TTL de cache en el browser para assets versionados de kkfileview. */
    private static final String IMMUTABLE_CACHE_HEADER = "public, max-age=3600, immutable";

    // ── Patrones pre-compilados (evitan recompilar en cada request) ────────────
    private static final Pattern RE_KK_JSON_URL    = Pattern.compile("\"url\":\"https?://[^\"]*:8012/");
    private static final Pattern RE_KK_JSON_URL_S  = Pattern.compile("\"url\": \"https?://[^\"]*:8012/");
    private static final Pattern RE_KK_JSON_THUMB  = Pattern.compile("\"thumbUrl\":\"https?://[^\"]*:8012/");
    private static final Pattern RE_KK_JSON_THUMB_S= Pattern.compile("\"thumbUrl\": \"https?://[^\"]*:8012/");
    private static final Pattern RE_KK_ATTR_DQUO   = Pattern.compile("(href|src|data-src)=\"https?://[^\"]*:8012/");
    private static final Pattern RE_KK_ATTR_SQUO   = Pattern.compile("(href|src|data-src)='https?://[^']*:8012/");

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

        // Headers de cache para assets que el browser puede reusar.
        // Solo para imágenes/JS/CSS — el HTML del viewer cambia entre documentos.
        String lowerCT = contentType == null ? "" : contentType.toLowerCase();
        boolean isHtml = lowerCT.contains("text/html");
        boolean isCss  = lowerCT.contains("text/css");
        boolean isJs   = lowerCT.contains("javascript") || lowerCT.contains("text/javascript");
        boolean isImg  = lowerCT.contains("image/");
        if (isCss || isJs || isImg) {
            response.setHeader("Cache-Control", IMMUTABLE_CACHE_HEADER);
        } else if (isHtml) {
            response.setHeader("Cache-Control", "no-cache");
        }

        try (InputStream is = upstream.body()) {
            if (isHtml || isCss) {
                rewriteAndWriteText(is, contentType, lowerCT, response);
            } else {
                streamBinary(is, upstream, response);
            }
        }
    }

    /**
     * Lee texto, reescribe paths absolutos (si los hay) y escribe.
     * Para CSS, los paths suelen ser relativos así que se chequea primero si
     * hay algo que reescribir — si no, se evita el costo del regex.
     */
    private void rewriteAndWriteText(InputStream is,
                                     String contentType,
                                     String lowerCT,
                                     HttpServletResponse response) throws IOException {
        byte[] buf = readAllBytes(is);
        if (buf.length == 0) {
            response.setContentLength(0);
            return;
        }

        // Quick scan: si no contiene ":8012" ni "=\"/" ni "/static", no hay nada que reescribir.
        boolean mayNeedRewrite = false;
        for (int i = 0; i <= buf.length - 5; i++) {
            byte b = buf[i];
            if (b == ':' && i + 4 < buf.length
                    && buf[i + 1] == '8' && buf[i + 2] == '0'
                    && buf[i + 3] == '1' && buf[i + 4] == '2') {
                mayNeedRewrite = true; break;
            }
            if (b == '=' && i + 2 < buf.length && buf[i + 1] == '"' && buf[i + 2] == '/') {
                mayNeedRewrite = true; break;
            }
        }

        byte[] out;
        if (mayNeedRewrite) {
            String charset = extractCharset(contentType);
            String body = new String(buf, charset);
            body = rewriteAbsolutePaths(body);
            String baseType = contentType.split(";")[0].trim();
            response.setContentType(baseType + ";charset=UTF-8");
            out = body.getBytes(StandardCharsets.UTF_8);
        } else {
            // No había nada que reescribir — emite los bytes crudos.
            // Aseguramos charset en HTML.
            if (lowerCT.contains("text/html") && !lowerCT.contains("charset")) {
                response.setContentType(contentType.split(";")[0].trim() + ";charset=UTF-8");
            }
            out = buf;
        }

        response.setContentLengthLong(out.length);
        response.getOutputStream().write(out);
    }

    /** Stream binario con buffer grande y Content-Length cuando upstream lo provee. */
    private void streamBinary(InputStream is,
                              HttpResponse<?> upstream,
                              HttpServletResponse response) throws IOException {
        long contentLength = upstream.headers().firstValueAsLong("content-length").orElse(-1L);
        if (contentLength > 0) {
            response.setContentLengthLong(contentLength);
        }
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        int n;
        while ((n = is.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        out.flush();
    }

    /** Lee todos los bytes del stream (HTML/CSS son pequeños — pocos KB). */
    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        int n;
        while ((n = is.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    /**
     * Reescribe rutas absolutas (que empiecen con /) para que pasen por el proxy.
     * Las rutas relativas (sin /) ya resuelven correctamente al estar la página
     * bajo /api/documentos/kk-view/.
     * La única ruta absoluta en los templates es /static/bootstrap/js/html5shiv.js
     * dentro de un comentario condicional IE, ignorado por navegadores modernos.
     */
    private String rewriteAbsolutePaths(String body) {
        body = RE_KK_JSON_URL.matcher(body).replaceAll("\"url\":\""       + PROXY_PREFIX + "/");
        body = RE_KK_JSON_URL_S.matcher(body).replaceAll("\"url\": \""      + PROXY_PREFIX + "/");
        body = RE_KK_JSON_THUMB.matcher(body).replaceAll("\"thumbUrl\":\""  + PROXY_PREFIX + "/");
        body = RE_KK_JSON_THUMB_S.matcher(body).replaceAll("\"thumbUrl\": \"" + PROXY_PREFIX + "/");
        body = RE_KK_ATTR_DQUO.matcher(body).replaceAll("$1=\""     + PROXY_PREFIX + "/");
        body = RE_KK_ATTR_SQUO.matcher(body).replaceAll("$1='"      + PROXY_PREFIX + "/");
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

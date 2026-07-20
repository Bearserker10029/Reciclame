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

// Reverse proxy hacia kkFileView (visor de Office/PDF). kkFileView solo es
// alcanzable dentro de la red interna de Docker (puerto 8012, sin auth),
// así que todo lo que le pide el navegador pasa por acá primero, protegido
// por @PreAuthorize. El HTML/CSS/JSON que kkFileView devuelve trae URLs
// absolutas apuntando a sí mismo (http://...:8012/...) — rewriteAbsolutePaths
// las reescribe hacia este proxy, si no el navegador intentaría pegarle
// directo al puerto interno y fallaría.
@RestController
@RequestMapping("/api/documentos/kk-view")
public class KkfileviewViewerProxyController {

    static final String PROXY_PREFIX = "/api/documentos/kk-view";

    private static final int STREAM_BUFFER_BYTES = 256 * 1024;

    private static final String IMMUTABLE_CACHE_HEADER = "public, max-age=3600, immutable";

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
                .timeout(Duration.ofMinutes(10))
                .GET();

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

    private void rewriteAndWriteText(InputStream is,
                                     String contentType,
                                     String lowerCT,
                                     HttpServletResponse response) throws IOException {
        byte[] buf = readAllBytes(is);
        if (buf.length == 0) {
            response.setContentLength(0);
            return;
        }

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
            if (lowerCT.contains("text/html") && !lowerCT.contains("charset")) {
                response.setContentType(contentType.split(";")[0].trim() + ";charset=UTF-8");
            }
            out = buf;
        }

        response.setContentLengthLong(out.length);
        response.getOutputStream().write(out);
    }

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

    private byte[] readAllBytes(InputStream is) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        int n;
        while ((n = is.read(buffer)) != -1) {
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    private String rewriteAbsolutePaths(String body) {
        body = RE_KK_JSON_URL.matcher(body).replaceAll("\"url\":\""       + PROXY_PREFIX + "/");
        body = RE_KK_JSON_URL_S.matcher(body).replaceAll("\"url\": \""      + PROXY_PREFIX + "/");
        body = RE_KK_JSON_THUMB.matcher(body).replaceAll("\"thumbUrl\":\""  + PROXY_PREFIX + "/");
        body = RE_KK_JSON_THUMB_S.matcher(body).replaceAll("\"thumbUrl\": \"" + PROXY_PREFIX + "/");
        body = RE_KK_ATTR_DQUO.matcher(body).replaceAll("$1=\""     + PROXY_PREFIX + "/");
        body = RE_KK_ATTR_SQUO.matcher(body).replaceAll("$1='"      + PROXY_PREFIX + "/");
        body = body.replace("href=\"/",    "href=\""    + PROXY_PREFIX + "/");
        body = body.replace("href='/",     "href='"     + PROXY_PREFIX + "/");
        body = body.replace("src=\"/",     "src=\""     + PROXY_PREFIX + "/");
        body = body.replace("src='/",      "src='"      + PROXY_PREFIX + "/");
        body = body.replace("data-src=\"/","data-src=\""+ PROXY_PREFIX + "/");
        body = body.replace("data-src='/", "data-src='" + PROXY_PREFIX + "/");
        body = body.replace("action=\"/",  "action=\""  + PROXY_PREFIX + "/");
        body = body.replace("url('/",      "url('"      + PROXY_PREFIX + "/");
        body = body.replace("url(\"/",     "url(\""     + PROXY_PREFIX + "/");
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

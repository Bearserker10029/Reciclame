package org.reciclame.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.url}")
    private String webhookUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Path del login (canónico). Configurable vía {@code app.login-path}.
     * Por defecto: {@code /service/repositorio/login}, alineado con el patrón
     * del menú "Ejes de Gestión +" del sitio público.
     */
    @Value("${app.login-path:/service/repositorio/login}")
    private String loginPath;

    public EmailNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORE
    // ══════════════════════════════════════════════════════════════════════

    public void sendEmail(String to, String subject, String message) {
        var payload = Map.of("to", to, "subject", subject, "message", message);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(webhookUrl, new HttpEntity<>(payload, headers), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[Email] Enviado correctamente a {} | asunto: '{}'", to, subject);
            } else {
                log.warn("[Email] n8n respondio con estado inesperado: {}", response.getStatusCode());
            }
        } catch (RestClientException ex) {
            log.error("[Email] Fallo al contactar n8n ({}): {}", webhookUrl, ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // GESTIÓN DE CUENTA
    // ══════════════════════════════════════════════════════════════════════

    public void enviarBienvenida(String to, String nombreCompleto) {
        sendEmail(to, "Bienvenido a Recíclame", html(
                "👋 ¡Bienvenido, " + nombreCompleto + "!",
                "#2ECC71",
                """
                <p style="margin:0 0 16px">Tu cuenta ha sido creada exitosamente en la plataforma
                <strong>Recíclame</strong>.</p>
                <p style="margin:0">Pronto un administrador revisará tu solicitud y recibirás
                la confirmación de aprobación.</p>
                """,
                null, null
        ));
    }

    public void enviarAprobacion(String to, String nombreCompleto) {
        sendEmail(to, "✅ Tu cuenta en Recíclame fue aprobada", html(
                "¡Todo listo, " + nombreCompleto + "!",
                "#2ECC71",
                """
                <p style="margin:0 0 16px">Tu cuenta en <strong>Recíclame</strong> ha sido
                <strong style="color:#2ECC71">aprobada</strong>. Ya puedes acceder con tus credenciales.</p>
                """,
                "Iniciar sesión",
                baseUrl + loginPath
        ));
    }

    public void enviarRechazo(String to, String nombreCompleto, String motivo) {
        sendEmail(to, "Estado de tu solicitud en Recíclame", html(
                "Hola, " + nombreCompleto,
                "#E74C3C",
                """
                <p style="margin:0 0 12px">Lamentamos informarte que tu solicitud fue
                <strong style="color:#E74C3C">rechazada</strong>.</p>
                <div style="background:#fdf2f2;border-left:4px solid #E74C3C;padding:12px 16px;
                     border-radius:4px;margin:0 0 12px">
                  <strong>Motivo:</strong> """ + escHtml(motivo) + """
                </div>
                <p style="margin:0">Si tienes dudas, contáctate con el administrador.</p>
                """,
                null, null
        ));
    }

    public void enviarBloqueo(String to, String nombreCompleto) {
        sendEmail(to, "⚠️ Cuenta bloqueada — Recíclame", html(
                "Hola, " + nombreCompleto,
                "#E67E22",
                """
                <p style="margin:0 0 12px">Tu cuenta en <strong>Recíclame</strong> ha sido
                <strong style="color:#E67E22">bloqueada temporalmente</strong>.</p>
                <p style="margin:0">Comunícate con el administrador de tu organización para
                obtener más información.</p>
                """,
                null, null
        ));
    }

    public void enviarReactivacion(String to, String nombreCompleto) {
        sendEmail(to, "✅ Tu cuenta fue reactivada — Recíclame", html(
                "¡Bienvenido de vuelta, " + nombreCompleto + "!",
                "#2ECC71",
                """
                <p style="margin:0 0 16px">Tu cuenta en <strong>Recíclame</strong> ha sido
                <strong style="color:#2ECC71">reactivada</strong>. Ya puedes ingresar normalmente.</p>
                """,
                "Iniciar sesión",
                baseUrl + loginPath
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // RECUPERACIÓN DE CONTRASEÑA
    // ══════════════════════════════════════════════════════════════════════

    public void enviarRecuperacionContrasena(String to, String nombreCompleto, String linkReset) {
        sendEmail(to, "🔑 Recuperación de contraseña — Recíclame", html(
                "Restablecer contraseña",
                "#1B3A5C",
                """
                <p style="margin:0 0 12px">Hola <strong>""" + escHtml(nombreCompleto) + """
                </strong>, recibimos una solicitud para restablecer la contraseña de tu cuenta.</p>
                <p style="margin:0 0 20px">Haz clic en el botón a continuación. El enlace expira
                en <strong>1 hora</strong>.</p>
                <p style="margin:0;color:#888;font-size:13px">Si no solicitaste este cambio,
                ignora este correo. Tu contraseña no será modificada.</p>
                """,
                "Restablecer contraseña",
                linkReset
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONTENIDO — NORMATIVAS Y ESTUDIOS
    // ══════════════════════════════════════════════════════════════════════

    public void enviarAlertaNormativa(String to, String tituloNormativa) {
        sendEmail(to, "📋 Nueva normativa publicada — Recíclame", html(
                "Nuevo contenido legal disponible",
                "#1B3A5C",
                """
                <p style="margin:0 0 12px">Se ha publicado una nueva normativa en la plataforma
                <strong>Recíclame</strong> que puede ser de tu interés:</p>
                <div style="background:#EBF5FB;border-left:4px solid #1B3A5C;padding:14px 18px;
                     border-radius:6px;margin:0 0 20px">
                  <strong style="color:#1B3A5C;font-size:15px">""" + escHtml(tituloNormativa) + """
                  </strong>
                </div>
                <p style="margin:0 0 20px">Accede a la plataforma para revisar el documento completo.</p>
                """,
                "Ver normativa",
                baseUrl + loginPath
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // SOPORTE
    // ══════════════════════════════════════════════════════════════════════

    public void enviarConfirmacionSoporte(String to, String ticketId) {
        sendEmail(to, "🎫 Ticket de soporte recibido — Recíclame", html(
                "Recibimos tu solicitud",
                "#8E44AD",
                """
                <p style="margin:0 0 12px">Hemos recibido tu mensaje de soporte correctamente.
                Nuestro equipo lo revisará a la brevedad.</p>
                <div style="background:#F5EEF8;border-left:4px solid #8E44AD;padding:14px 18px;
                     border-radius:6px;margin:0 0 20px">
                  <span style="color:#888;font-size:12px;text-transform:uppercase;
                        letter-spacing:1px">Número de ticket</span><br>
                  <strong style="color:#8E44AD;font-size:20px;letter-spacing:2px">"""
                        + escHtml(ticketId) + """
                  </strong>
                </div>
                <p style="margin:0;color:#888;font-size:13px">Guarda este número para hacer
                seguimiento de tu solicitud.</p>
                """,
                null, null
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // PLANTILLA HTML MAESTRA
    // ══════════════════════════════════════════════════════════════════════

    private String html(String titulo, String colorAccent, String cuerpo,
                        String btnTexto, String btnUrl) {
        String boton = (btnTexto != null && btnUrl != null) ? """
                <div style="text-align:center;margin:28px 0 0">
                  <a href="%s"
                     style="display:inline-block;background:%s;color:#fff;text-decoration:none;
                            font-weight:700;font-size:15px;padding:14px 36px;border-radius:8px;
                            letter-spacing:.3px">%s</a>
                </div>
                """.formatted(btnUrl, colorAccent, escHtml(btnTexto)) : "";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#F0F4F8;font-family:'Segoe UI',Arial,sans-serif">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#F0F4F8;padding:40px 0">
                    <tr><td align="center">
                      <table width="580" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 4px 24px rgba(0,0,0,.08)">

                        <!-- HEADER -->
                        <tr>
                          <td style="background:%s;padding:32px 40px;text-align:center">
                            <div style="color:#fff;font-size:22px;font-weight:800;letter-spacing:-.3px">
                              ♻️ Recíclame
                            </div>
                            <div style="color:rgba(255,255,255,.75);font-size:13px;margin-top:4px">
                              Plataforma de Gestión Ambiental
                            </div>
                          </td>
                        </tr>

                        <!-- TITLE -->
                        <tr>
                          <td style="padding:32px 40px 0">
                            <h1 style="margin:0;font-size:20px;font-weight:700;color:#1a2b3c">%s</h1>
                            <hr style="border:none;border-top:2px solid %s;margin:16px 0 0;width:48px;
                                       margin-left:0">
                          </td>
                        </tr>

                        <!-- BODY -->
                        <tr>
                          <td style="padding:20px 40px 0;color:#4a5568;font-size:15px;line-height:1.7">
                            %s
                            %s
                          </td>
                        </tr>

                        <!-- FOOTER -->
                        <tr>
                          <td style="padding:32px 40px;border-top:1px solid #EDF2F7;margin-top:32px">
                            <p style="margin:0;color:#A0AEC0;font-size:12px;line-height:1.6">
                              Este correo fue enviado automáticamente por la plataforma
                              <strong>Recíclame</strong>. Por favor no respondas a este mensaje.<br>
                              © 2026 Recíclame Perú — Todos los derechos reservados.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(colorAccent, titulo, colorAccent, cuerpo, boton);
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

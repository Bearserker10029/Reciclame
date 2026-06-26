package org.reciclame.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from:Reciclame <reciclameecodevs@gmail.com>}")
    private String fromAddress;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.login-path:/service/repositorio/login}")
    private String loginPath;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORE
    // ══════════════════════════════════════════════════════════════════════

    public void sendEmail(String to, String subject, String message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(message, true);
            mailSender.send(mime);
            log.info("[Email] Enviado correctamente a {} | asunto: '{}'", to, subject);
        } catch (MessagingException ex) {
            log.error("[Email] Fallo al enviar a {}: {}", to, ex.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FLUJOS
    // ══════════════════════════════════════════════════════════════════════

    public void enviarBienvenida(String to, String nombreCompleto) {
        sendEmail(to, "Solicitud recibida — Recíclame", html(
            "¡Hola, " + esc(nombreCompleto) + "!",
            "Tu solicitud fue recibida exitosamente",
            "#1B3A5C",
            """
            <p>Gracias por registrarte en la plataforma <strong>Recíclame</strong>.
            Somos una iniciativa comprometida con la <strong>economía circular</strong>
            y la gestión responsable de residuos en el Perú.</p>

            <p>Tu solicitud está siendo revisada por nuestro equipo de administración.
            Te notificaremos por este medio cuando sea aprobada.</p>

            <div style="background:#F0FFF4;border-left:4px solid #2ECC71;border-radius:0 8px 8px 0;
                        padding:16px 20px;margin:24px 0">
              <p style="margin:0;color:#276749;font-size:14px;font-weight:600">
                ⏳ Tiempo estimado de revisión: 1 a 2 días hábiles
              </p>
            </div>

            <p style="color:#718096;font-size:13px">
              Si tienes alguna consulta, puedes comunicarte con el equipo de Recíclame
              respondiendo a este correo.
            </p>
            """,
            null, null
        ));
    }

    public void enviarCredenciales(String to, String nombreCompleto, String correo, String passwordTemporal) {
        sendEmail(to, "¡Tu cuenta fue aprobada! — Recíclame", html(
            "¡Bienvenido a Recíclame, " + esc(nombreCompleto) + "!",
            "Tu cuenta está lista para usar",
            "#1B3A5C",
            """
            <p>Nos complace informarte que tu solicitud ha sido
            <strong style="color:#2ECC71">aprobada</strong>.
            Ya puedes acceder a la plataforma con las siguientes credenciales:</p>

            <div style="background:#F7FAFC;border:1px solid #E2E8F0;border-radius:12px;
                        padding:24px 28px;margin:24px 0">
              <p style="margin:0 0 16px;color:#4A5568;font-size:13px;font-weight:600;
                         text-transform:uppercase;letter-spacing:1px">Tus credenciales de acceso</p>
              <table style="width:100%%;border-collapse:collapse">
                <tr>
                  <td style="padding:12px 0;border-bottom:1px solid #EDF2F7;
                             color:#718096;font-size:13px;width:160px;vertical-align:top">
                    Correo electrónico
                  </td>
                  <td style="padding:12px 0;border-bottom:1px solid #EDF2F7;
                             font-family:monospace;font-size:14px;color:#1B3A5C;font-weight:600">
            """ + esc(correo) + """
                  </td>
                </tr>
                <tr>
                  <td style="padding:12px 0;color:#718096;font-size:13px;vertical-align:middle">
                    Contraseña temporal
                  </td>
                  <td style="padding:12px 0">
                    <span style="background:#1B3A5C;color:#2ECC71;font-family:monospace;
                                 font-size:20px;font-weight:800;letter-spacing:4px;
                                 padding:10px 20px;border-radius:8px;display:inline-block">
            """ + esc(passwordTemporal) + """
                    </span>
                  </td>
                </tr>
              </table>
            </div>

            <div style="background:#FFF5F5;border-left:4px solid #FC8181;border-radius:0 8px 8px 0;
                        padding:14px 18px;margin-bottom:24px">
              <p style="margin:0;color:#C53030;font-size:13px;font-weight:600">
                🔒 Por seguridad, te recomendamos cambiar tu contraseña al iniciar sesión por primera vez.
              </p>
            </div>
            """,
            "Iniciar sesión ahora",
            baseUrl + loginPath
        ));
    }

    public void enviarAprobacion(String to, String nombreCompleto) {
        sendEmail(to, "Cuenta aprobada — Recíclame", html(
            "¡Todo listo, " + esc(nombreCompleto) + "!",
            "Tu cuenta fue aprobada",
            "#1B3A5C",
            """
            <p>Tu cuenta en <strong>Recíclame</strong> ha sido
            <strong style="color:#2ECC71">aprobada</strong> exitosamente.</p>
            <p>Ya puedes acceder a la plataforma con tus credenciales y comenzar
            a gestionar tus actividades de reciclaje.</p>
            <div style="background:#F0FFF4;border-left:4px solid #2ECC71;border-radius:0 8px 8px 0;
                        padding:14px 18px;margin:24px 0">
              <p style="margin:0;color:#276749;font-size:13px">
                Recuerda mantener tu información actualizada y revisar periódicamente
                las normativas publicadas en la plataforma.
              </p>
            </div>
            """,
            "Iniciar sesión",
            baseUrl + loginPath
        ));
    }

    public void enviarRechazo(String to, String nombreCompleto, String motivo) {
        sendEmail(to, "Resultado de tu solicitud — Recíclame", html(
            "Hola, " + esc(nombreCompleto),
            "Tu solicitud no pudo ser aprobada",
            "#C53030",
            """
            <p>Lamentamos informarte que tu solicitud de acceso a la plataforma
            <strong>Recíclame</strong> no pudo ser aprobada en esta oportunidad.</p>

            <div style="background:#FFF5F5;border:1px solid #FED7D7;border-radius:12px;
                        padding:20px 24px;margin:24px 0">
              <p style="margin:0 0 8px;color:#718096;font-size:12px;font-weight:600;
                         text-transform:uppercase;letter-spacing:1px">Motivo de rechazo</p>
              <p style="margin:0;color:#2D3748;font-size:15px;line-height:1.6">
            """ + esc(motivo) + """
              </p>
            </div>

            <p style="color:#718096;font-size:13px">
              Si consideras que esto es un error o deseas mayor información,
              comunícate directamente con el administrador de tu organización.
            </p>
            """,
            null, null
        ));
    }

    public void enviarBloqueo(String to, String nombreCompleto) {
        sendEmail(to, "Cuenta suspendida — Recíclame", html(
            "Hola, " + esc(nombreCompleto),
            "Tu cuenta ha sido suspendida temporalmente",
            "#D97706",
            """
            <p>Tu cuenta en la plataforma <strong>Recíclame</strong> ha sido
            <strong style="color:#D97706">suspendida temporalmente</strong>
            por el administrador.</p>

            <div style="background:#FFFBEB;border-left:4px solid #F6AD55;border-radius:0 8px 8px 0;
                        padding:16px 20px;margin:24px 0">
              <p style="margin:0;color:#92400E;font-size:14px">
                Si consideras que esto es un error o necesitas más información,
                comunícate con el administrador de tu organización para resolver
                la situación a la brevedad.
              </p>
            </div>

            <p style="color:#718096;font-size:13px">
              Durante el período de suspensión no podrás acceder a la plataforma.
            </p>
            """,
            null, null
        ));
    }

    public void enviarReactivacion(String to, String nombreCompleto) {
        sendEmail(to, "Cuenta reactivada — Recíclame", html(
            "¡Bienvenido de vuelta, " + esc(nombreCompleto) + "!",
            "Tu cuenta fue reactivada exitosamente",
            "#1B3A5C",
            """
            <p>Tu cuenta en la plataforma <strong>Recíclame</strong> ha sido
            <strong style="color:#2ECC71">reactivada</strong> exitosamente.</p>

            <p>Ya puedes acceder con normalidad y continuar gestionando tus
            actividades en la plataforma.</p>

            <div style="background:#F0FFF4;border-left:4px solid #2ECC71;border-radius:0 8px 8px 0;
                        padding:14px 18px;margin:24px 0">
              <p style="margin:0;color:#276749;font-size:13px">
                Si tienes alguna consulta sobre tu cuenta, no dudes en contactar
                al equipo de soporte de Recíclame.
              </p>
            </div>
            """,
            "Iniciar sesión",
            baseUrl + loginPath
        ));
    }

    public void enviarRecuperacionContrasena(String to, String nombreCompleto, String linkReset) {
        sendEmail(to, "Recupera tu contraseña — Recíclame", html(
            "Restablecer contraseña",
            "Solicitud de recuperación de acceso",
            "#1B3A5C",
            """
            <p>Hola <strong>""" + esc(nombreCompleto) + """
            </strong>, recibimos una solicitud para restablecer la contraseña
            de tu cuenta en <strong>Recíclame</strong>.</p>

            <p>Haz clic en el botón a continuación para crear una nueva contraseña.
            Este enlace es válido por <strong>1 hora</strong> a partir de la recepción
            de este correo.</p>

            <div style="background:#EBF8FF;border-left:4px solid #63B3ED;border-radius:0 8px 8px 0;
                        padding:14px 18px;margin:24px 0">
              <p style="margin:0;color:#2C5282;font-size:13px">
                🛡️ Si <strong>no solicitaste</strong> este cambio, ignora este correo.
                Tu contraseña actual seguirá siendo la misma y nadie más tiene acceso
                a tu cuenta.
              </p>
            </div>
            """,
            "Restablecer mi contraseña",
            linkReset
        ));
    }

    public void enviarAlertaNormativa(String to, String tituloNormativa) {
        sendEmail(to, "Nueva normativa publicada — Recíclame", html(
            "Nuevo contenido disponible",
            "Se publicó una actualización normativa",
            "#1B3A5C",
            """
            <p>Se ha publicado una nueva normativa en la plataforma
            <strong>Recíclame</strong> que puede ser de tu interés:</p>

            <div style="background:#EBF8FF;border:1px solid #BEE3F8;border-radius:12px;
                        padding:20px 24px;margin:24px 0">
              <p style="margin:0 0 6px;color:#718096;font-size:12px;font-weight:600;
                         text-transform:uppercase;letter-spacing:1px">Documento publicado</p>
              <p style="margin:0;color:#1B3A5C;font-size:16px;font-weight:700">
                📄 """ + esc(tituloNormativa) + """
              </p>
            </div>

            <p>Ingresa a la plataforma para revisar el documento completo y mantenerte
            actualizado con las últimas disposiciones.</p>
            """,
            "Ver normativa",
            baseUrl + loginPath
        ));
    }

    public void enviarReporteAdjunto(String to, String nombreCompleto, String tituloReporte,
                                     int totalRegistros, byte[] adjunto,
                                     String nombreArchivo, String tipoMime) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(tituloReporte + " — Recíclame");

            String cuerpo = html(
                "¡Hola, " + esc(nombreCompleto != null ? nombreCompleto : "") + "!",
                "Tu reporte está listo",
                "#1B3A5C",
                """
                <p>Adjuntamos el reporte que solicitaste desde la plataforma <strong>Recíclame</strong>.</p>

                <div style="background:#F0FFF4;border-left:4px solid #2ECC71;border-radius:0 8px 8px 0;
                            padding:16px 20px;margin:24px 0">
                  <p style="margin:0 0 4px;color:#276749;font-size:13px;font-weight:700;
                             text-transform:uppercase;letter-spacing:.5px">Detalles del reporte</p>
                  <p style="margin:0;color:#2D3748;font-size:14px"><strong>Reporte:</strong> %s</p>
                  <p style="margin:4px 0 0;color:#2D3748;font-size:14px"><strong>Registros:</strong> %d</p>
                  <p style="margin:4px 0 0;color:#2D3748;font-size:14px"><strong>Archivo:</strong> %s</p>
                </div>

                <p style="color:#718096;font-size:13px">
                  Este archivo contiene información corporativa de la plataforma.
                  Trátalo de forma confidencial.
                </p>
                """.formatted(esc(tituloReporte), totalRegistros, esc(nombreArchivo)),
                null, null
            );

            helper.setText(cuerpo, true);
            helper.addAttachment(nombreArchivo,
                    new org.springframework.core.io.ByteArrayResource(adjunto),
                    tipoMime);
            mailSender.send(mime);
            log.info("[Email] Reporte '{}' enviado a {} ({} bytes)", nombreArchivo, to, adjunto.length);
        } catch (MessagingException ex) {
            log.error("[Email] Fallo al enviar reporte a {}: {}", to, ex.getMessage());
            throw new RuntimeException("No se pudo enviar el correo: " + ex.getMessage(), ex);
        }
    }

    public void enviarConfirmacionSoporte(String to, String ticketId) {
        sendEmail(to, "Ticket de soporte recibido — Recíclame", html(
            "Recibimos tu solicitud de soporte",
            "Nuestro equipo te atenderá a la brevedad",
            "#6B46C1",
            """
            <p>Hemos recibido tu mensaje correctamente. Nuestro equipo de soporte
            técnico lo revisará y te contactará en el menor tiempo posible.</p>

            <div style="background:#FAF5FF;border:1px solid #D6BCFA;border-radius:12px;
                        padding:24px;margin:24px 0;text-align:center">
              <p style="margin:0 0 8px;color:#718096;font-size:12px;font-weight:600;
                         text-transform:uppercase;letter-spacing:1px">Número de ticket</p>
              <p style="margin:0;color:#6B46C1;font-size:32px;font-weight:800;
                         letter-spacing:5px;font-family:monospace">
            """ + esc(ticketId) + """
              </p>
            </div>

            <p style="color:#718096;font-size:13px">
              Guarda este número de ticket para hacer seguimiento de tu solicitud.
              Puedes mencionarlo en futuras comunicaciones con nuestro equipo.
            </p>
            """,
            null, null
        ));
    }

    // ══════════════════════════════════════════════════════════════════════
    // PLANTILLA HTML MAESTRA
    // ══════════════════════════════════════════════════════════════════════

    private String html(String saludo, String subtitulo, String accentColor,
                        String cuerpo, String btnTexto, String btnUrl) {

        String logoUrl = baseUrl + "/assets/images/reciclame/logo-blanco-Reciclame.png";

        String boton = (btnTexto != null && btnUrl != null) ? """
            <div style="text-align:center;margin:32px 0 8px">
              <a href="%s"
                 style="display:inline-block;background:linear-gradient(135deg,#2ECC71,#27AE60);
                        color:#fff;text-decoration:none;font-weight:700;font-size:15px;
                        padding:16px 44px;border-radius:50px;letter-spacing:.3px;
                        box-shadow:0 4px 18px rgba(46,204,113,.4)">
                %s &nbsp;→
              </a>
            </div>
            """.formatted(btnUrl, esc(btnTexto)) : "";

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1"/>
              <title>Recíclame</title>
            </head>
            <body style="margin:0;padding:0;background:#EDF2F7;font-family:'Segoe UI',Arial,sans-serif">

              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#EDF2F7;padding:40px 16px">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%">

                    <!-- HEADER CON LOGO -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#0F2236 0%%,#1B3A5C 100%%);
                                 border-radius:16px 16px 0 0;padding:32px 40px;text-align:center">
                        <img src="%s"
                             alt="Recíclame"
                             width="180"
                             style="max-width:180px;height:auto;display:block;margin:0 auto"/>
                        <p style="margin:12px 0 0;color:rgba(255,255,255,.5);font-size:11px;
                                   letter-spacing:3px;text-transform:uppercase">
                          Plataforma de Economía Circular
                        </p>
                      </td>
                    </tr>

                    <!-- BARRA DE ACENTO -->
                    <tr>
                      <td style="background:linear-gradient(90deg,%s,#2ECC71);height:4px;
                                 line-height:4px;font-size:0">&nbsp;</td>
                    </tr>

                    <!-- CARD BODY -->
                    <tr>
                      <td style="background:#fff;padding:40px 44px 32px">

                        <!-- SALUDO -->
                        <h1 style="margin:0 0 4px;color:#1B3A5C;font-size:24px;font-weight:800;
                                    line-height:1.3">
                          %s
                        </h1>
                        <p style="margin:0 0 28px;color:#2ECC71;font-size:13px;font-weight:600;
                                   text-transform:uppercase;letter-spacing:1px">
                          %s
                        </p>
                        <hr style="border:none;border-top:1px solid #EDF2F7;margin:0 0 28px"/>

                        <!-- CONTENIDO -->
                        <div style="color:#4A5568;font-size:15px;line-height:1.8">
                          %s
                        </div>

                        %s

                      </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                      <td style="background:#F7FAFC;border-radius:0 0 16px 16px;
                                 padding:24px 44px;border-top:1px solid #E2E8F0">
                        <table width="100%%" cellpadding="0" cellspacing="0">
                          <tr>
                            <td>
                              <p style="margin:0 0 4px;color:#1B3A5C;font-size:13px;font-weight:700">
                                Recíclame Perú
                              </p>
                              <p style="margin:0;color:#A0AEC0;font-size:12px;line-height:1.6">
                                Este mensaje fue generado automáticamente. Por favor no respondas directamente.<br>
                                © 2026 Recíclame — Todos los derechos reservados.
                              </p>
                            </td>
                            <td style="text-align:right;vertical-align:middle">
                              <span style="display:inline-block;background:#1B3A5C;color:#2ECC71;
                                          font-size:20px;width:40px;height:40px;border-radius:50%%;
                                          text-align:center;line-height:40px">♻</span>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- TAGLINE -->
                    <tr>
                      <td style="padding:20px 0;text-align:center">
                        <p style="margin:0;color:#A0AEC0;font-size:11px;letter-spacing:2px;
                                   text-transform:uppercase">
                          Hacia una economía circular sostenible
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>

            </body>
            </html>
            """.formatted(logoUrl, accentColor, saludo, subtitulo, cuerpo, boton);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

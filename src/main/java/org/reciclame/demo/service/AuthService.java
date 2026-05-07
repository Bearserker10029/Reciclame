package org.reciclame.demo.service;

import org.reciclame.demo.model.PasswordResetToken;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.PasswordResetTokenRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailNotificationService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthService(PasswordResetTokenRepository tokenRepository,
                       UsuarioRepository usuarioRepository,
                       EmailNotificationService emailService,
                       PasswordEncoder passwordEncoder) {
        this.tokenRepository    = tokenRepository;
        this.usuarioRepository  = usuarioRepository;
        this.emailService       = emailService;
        this.passwordEncoder    = passwordEncoder;
    }

    /**
     * Genera un token de recuperación y envía el correo.
     * Respuesta siempre genérica para no revelar si el correo existe.
     */
    public void solicitarRecuperacion(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoCorporativo(correo);
        if (usuarioOpt.isEmpty()) {
            log.warn("[Auth] Intento de recuperación con correo no registrado: {}", correo);
            return;
        }

        Usuario usuario = usuarioOpt.get();

        // Eliminar tokens anteriores del mismo usuario
        tokenRepository.deleteByUsuarioId(usuario.getId());

        // Generar nuevo token con expiración de 1 hora
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUsuario(usuario);
        prt.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        tokenRepository.save(prt);

        String link = baseUrl + "/auth/restablecer?token=" + token;
        emailService.enviarRecuperacionContrasena(correo, usuario.getNombreCompleto(), link);
        log.info("[Auth] Enlace de recuperación enviado a {}", correo);
    }

    /**
     * Valida que el token exista, no esté usado y no haya expirado.
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> validarToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isUsado())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }

    /**
     * Cambia la contraseña y marca el token como usado.
     */
    public void restablecerContrasena(String token, String nuevaContrasena) {
        PasswordResetToken prt = validarToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        Usuario usuario = prt.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);

        prt.setUsado(true);
        tokenRepository.save(prt);
        log.info("[Auth] Contraseña restablecida para {}", usuario.getCorreoCorporativo());
    }
}

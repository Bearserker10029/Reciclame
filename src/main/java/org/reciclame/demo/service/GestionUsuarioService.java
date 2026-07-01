package org.reciclame.demo.service;

import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

@Service
public class GestionUsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final ParametrosSistemaService parametrosService;

    public GestionUsuarioService(UsuarioRepository usuarioRepository,
                                 ParametrosSistemaService parametrosService) {
        this.usuarioRepository = usuarioRepository;
        this.parametrosService = parametrosService;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoCorporativo(correo)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con correo: " + correo));

        if (!Boolean.TRUE.equals(usuario.getEstadoActivo())) {
            throw new UsernameNotFoundException("Usuario desactivado: " + correo);
        }

        String estado = usuario.getEstadoCuenta();
        if ("BLOQUEADO".equals(estado)) {
            // Auto-desbloqueo: si ya pasó parametros_sistema.tiempo_bloqueo_minutos
            // desde que se bloqueó la cuenta, se desbloquea sola y el login continúa.
            int minutosBloqueo = parametrosService.getIntegerOrDefault("tiempo_bloqueo_minutos", 30);
            Instant fechaBloqueo = usuario.getFechaBloqueo();
            boolean yaVencio = fechaBloqueo != null
                    && Instant.now().isAfter(fechaBloqueo.plusSeconds(minutosBloqueo * 60L));
            if (yaVencio) {
                usuarioRepository.resetearIntentosYDesbloquear(usuario.getId());
                estado = "ACTIVO";
                usuario.setEstadoCuenta("ACTIVO");
                usuario.setCuentaBloqueada(false);
            } else {
                throw new UsernameNotFoundException("Cuenta bloqueada para: " + correo);
            }
        }
        if ("INACTIVO".equals(estado)) {
            throw new UsernameNotFoundException("Cuenta inactiva para: " + correo);
        }

        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())
        );

        return new User(
                usuario.getCorreoCorporativo(),
                usuario.getPasswordHash(),
                "ACTIVO".equals(estado),
                true,
                true,
                true,
                authorities
        );
    }
}
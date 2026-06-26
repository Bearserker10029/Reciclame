package org.reciclame.demo.service;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RegistroService {

    private final SolicitudesRegistroRepository solicitudesRepo;
    private final UsuarioRepository usuarioRepo;

    public RegistroService(SolicitudesRegistroRepository solicitudesRepo,
                           UsuarioRepository usuarioRepo) {
        this.solicitudesRepo = solicitudesRepo;
        this.usuarioRepo = usuarioRepo;
    }

    private static final List<EstadoSolicitud> ESTADOS_ACTIVOS =
            List.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADA);

    public boolean canRegister(String correo, String dni) {
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.PENDIENTE)) return false;
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.APROBADA)
                && usuarioRepo.existsByCorreoCorporativoAndEstadoActivoTrue(correo)) return false;
        if (usuarioRepo.existsByCorreoCorporativoAndEstadoActivoTrue(correo)) return false;
        if (solicitudesRepo.existsByDniAndEstadoIn(dni, ESTADOS_ACTIVOS)
                && usuarioRepo.existsByDniAndEstadoActivoTrue(dni)) return false;
        if (usuarioRepo.existsByDniAndEstadoActivoTrue(dni)) return false;
        return true;
    }

    public String getStatusMessage(String correo, String dni) {
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.PENDIENTE))
            return "Ya existe una solicitud pendiente. Espera aprobación.";
        if (usuarioRepo.existsByCorreoCorporativoAndEstadoActivoTrue(correo))
            return "Ya tienes una cuenta activa. Inicia sesión.";
        if (usuarioRepo.existsByDniAndEstadoActivoTrue(dni))
            return "Este DNI ya está registrado.";
        return "";
    }
}

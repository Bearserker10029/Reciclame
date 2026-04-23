package org.reciclame.demo.service;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public boolean canRegister(String correo, String dni) {
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.PENDIENTE)) return false;
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.APROBADA)) return false;
        if (usuarioRepo.existsByCorreoCorporativo(correo)) return false;
        if (solicitudesRepo.existsByDni(dni)) return false;
        if (usuarioRepo.existsByDni(dni)) return false;
        return true;
    }

    public String getStatusMessage(String correo, String dni) {
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.PENDIENTE))
            return "Ya existe una solicitud pendiente. Espera aprobación.";
        if (solicitudesRepo.existsByCorreoCorporativoAndEstado(correo, EstadoSolicitud.APROBADA)
                || usuarioRepo.existsByCorreoCorporativo(correo))
            return "Ya tienes una cuenta activa. Inicia sesión.";
        if (solicitudesRepo.existsByDni(dni) || usuarioRepo.existsByDni(dni))
            return "Este DNI ya está registrado.";
        return "";
    }
}

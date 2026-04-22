package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UsuarioResponseDTO {

    private Long id;
    private String nombreCompleto;
    private String dni;
    private String correoCorporativo;
    private String cargo;
    private String fotoUrl;
    private Long rolId;
    private String rolNombre;
    private Long empresaId;
    private String empresaNombre;
    private String estadoCuenta;   // PENDIENTE | ACTIVO | INACTIVO
    private Boolean estadoActivo;
    private Boolean cuentaBloqueada;
    private Boolean tfaActivo;
    private Instant ultimoLogin;
    private Instant createdAt;
    private Instant updatedAt;
    // passwordHash nunca se expone aquí , por seguridad
}

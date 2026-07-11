package org.reciclame.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class SolicitudRegistroDTO {

    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 200, message = "El nombre completo no puede superar los 200 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @NotBlank(message = "El correo corporativo es obligatorio")
    @Email(message = "El correo corporativo debe tener un formato válido")
    @Size(max = 200, message = "El correo corporativo no puede superar los 200 caracteres")
    private String correoCorporativo;

    @Size(max = 200, message = "El nombre de la empresa no puede superar los 200 caracteres")
    private String empresaNombre;

    private String estado;
    private String motivoRechazo;
    private Instant fechaResolucion;
    private String revisadaPorNombre;
    private Instant createdAt;
}
package org.reciclame.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequestDTO {

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

    // Texto plano — el Service lo hashea. Validamos tamaño para impedir
    // passwords absurdamente largos antes de llegar a BCrypt.
    @Size(min = 6, max = 128, message = "La contraseña debe tener entre 6 y 128 caracteres")
    private String password;

    @Size(max = 150, message = "El cargo no puede superar los 150 caracteres")
    private String cargo;

    @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
    private String fotoUrl;

    @NotNull(message = "El rol es obligatorio")
    private Long rolId; // ID del rol (debe ser obligatorio al crear)

    private Long empresaId; //Id de la empresa
}

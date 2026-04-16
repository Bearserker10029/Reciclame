package org.reciclame.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequestDTO {

    private String nombreCompleto;
    private String dni;
    private String correoCorporativo;
    private String password;       // texto plano — el Service lo hashea
    private String cargo;
    private String fotoUrl;
    private Long rolId; // ID del rol (debe ser obligatorio al crear)
    private Long empresaId; //Id de la empresa
}

package org.reciclame.demo.dto;

import java.io.Serial;
import java.io.Serializable;

public class UsuarioSesion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombreCompleto;
    private String correoCorporativo;
    private String rolNombre;
    private String cargo;
    private String fotoUrl;
    private boolean debeCambiarPassword;

    public UsuarioSesion() {}

    public UsuarioSesion(Long id, String nombreCompleto, String correoCorporativo,
                         String rolNombre, String cargo, String fotoUrl) {
        this(id, nombreCompleto, correoCorporativo, rolNombre, cargo, fotoUrl, false);
    }

    public UsuarioSesion(Long id, String nombreCompleto, String correoCorporativo,
                         String rolNombre, String cargo, String fotoUrl, boolean debeCambiarPassword) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correoCorporativo = correoCorporativo;
        this.rolNombre = rolNombre;
        this.cargo = cargo;
        this.fotoUrl = fotoUrl;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreoCorporativo() { return correoCorporativo; }
    public void setCorreoCorporativo(String correoCorporativo) { this.correoCorporativo = correoCorporativo; }

    public String getRolNombre() { return rolNombre; }
    public void setRolNombre(String rolNombre) { this.rolNombre = rolNombre; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
}

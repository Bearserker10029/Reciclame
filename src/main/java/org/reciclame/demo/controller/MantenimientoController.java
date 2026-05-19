package org.reciclame.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MantenimientoController {

    @GetMapping("/mantenimiento")
    public String mantenimiento(Model model, Authentication auth) {
        String homeUrl = "/login";
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                switch (ga.getAuthority()) {
                    case "ROLE_SUPERADMIN":
                        homeUrl = "/superadmin/inicio";
                        break;
                    case "ROLE_ADMINISTRADOR":
                        homeUrl = "/admin/inicio";
                        break;
                    case "ROLE_SOCIO":
                    case "ROLE_IMPERSONATOR":
                        homeUrl = "/socio/inicio";
                        break;
                    case "ROLE_VISUALIZADOR":
                        homeUrl = "/visualizador/inicio";
                        break;
                }
            }
        }
        model.addAttribute("homeUrl", homeUrl);
        return "mantenimiento";
    }
}

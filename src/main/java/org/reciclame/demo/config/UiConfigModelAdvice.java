package org.reciclame.demo.config;

import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Inyecta la configuración de módulos del sidebar por rol en el modelo.
 */
@ControllerAdvice
public class UiConfigModelAdvice {

    private final ConfiguracionUiRolService service;

    public UiConfigModelAdvice(ConfiguracionUiRolService service) {
        this.service = service;
    }

    @ModelAttribute
    public void addUiConfig(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;

        String rol = resolveRol(auth);
        if (rol == null) {
            model.addAttribute("uiModulosSidebar", (Object) null);
            return;
        }

        model.addAttribute("uiModulosSidebar", service.getModulosSidebar(rol));
    }

    private String resolveRol(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            switch (ga.getAuthority()) {
                case "ROLE_SOCIO":        return "SOCIO";
                case "ROLE_VISUALIZADOR": return "VISUALIZADOR";
                case "ROLE_ADMINISTRADOR":return "ADMINISTRADOR";
                case "ROLE_IMPERSONATOR": return "SOCIO";
            }
        }
        return null;
    }
}

package org.reciclame.demo.config;

import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Inyecta la configuración de UI (columnas visibles, módulos del sidebar)
 * en el modelo de cada request de socio/visualizador.
 * Para admin/superadmin los atributos quedan null → se muestran todos los elementos.
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
            // Admin/SuperAdmin: no restricciones, atributos en null
            model.addAttribute("uiColsNormativas", (Object) null);
            model.addAttribute("uiColsEstudios",   (Object) null);
            model.addAttribute("uiModulosSidebar", (Object) null);
            return;
        }

        model.addAttribute("uiColsNormativas", service.getColumnasNormativas(rol));
        model.addAttribute("uiColsEstudios",   service.getColumnasEstudios(rol));
        model.addAttribute("uiModulosSidebar", service.getModulosSidebar(rol));
    }

    private String resolveRol(Authentication auth) {
        for (GrantedAuthority ga : auth.getAuthorities()) {
            switch (ga.getAuthority()) {
                case "ROLE_SOCIO":        return "SOCIO";
                case "ROLE_VISUALIZADOR": return "VISUALIZADOR";
                case "ROLE_IMPERSONATOR": return "SOCIO";
            }
        }
        return null;
    }
}

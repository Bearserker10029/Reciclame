package org.reciclame.demo.config;

import org.reciclame.demo.service.ConfiguracionUiRolService;
import org.reciclame.demo.service.ParametrosSistemaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;

/**
 * Inyecta la configuración de módulos del sidebar por rol en el modelo.
 * Blindado: cualquier excepcion del service no debe tumbar todas las vistas.
 */
@ControllerAdvice
public class UiConfigModelAdvice {

    private final ConfiguracionUiRolService service;
    private final ParametrosSistemaService parametrosService;

    @Value("${chatbot.enabled:true}")
    private boolean chatbotEnabledDefault;

    public UiConfigModelAdvice(ConfiguracionUiRolService service, ParametrosSistemaService parametrosService) {
        this.service = service;
        this.parametrosService = parametrosService;
    }

    @ModelAttribute
    public void addUiConfig(Model model) {
        // parametros_sistema.chatbot_habilitado (editable por SuperAdmin) manda sobre
        // el valor de application.properties, que queda como fallback.
        Boolean chatbotHabilitado = parametrosService.getBoolean("chatbot_habilitado");
        model.addAttribute("chatbotHabilitado",
                chatbotHabilitado != null ? chatbotHabilitado : chatbotEnabledDefault);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return;

            String rol = resolveRol(auth);
            if (rol == null) {
                model.addAttribute("uiModulosSidebar", (Object) null);
                return;
            }

            Object modulos = service.getModulosSidebar(rol);
            // El template hace <th:block th:if="${uiModulosSidebar != null and ...}">,
            // asi que Collections.emptyList() es seguro -> la vista no renderiza el bloque.
            model.addAttribute("uiModulosSidebar", modulos != null ? modulos : Collections.emptyList());
        } catch (Exception ex) {
            System.err.println("[UiConfigModelAdvice] No se pudo cargar uiModulosSidebar: " + ex.getMessage());
            model.addAttribute("uiModulosSidebar", Collections.emptyList());
        }
    }

    private String resolveRol(Authentication auth) {
        try {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga == null || ga.getAuthority() == null) continue;
                switch (ga.getAuthority()) {
                    case "ROLE_SOCIO":        return "SOCIO";
                    case "ROLE_VISUALIZADOR": return "VISUALIZADOR";
                    case "ROLE_ADMINISTRADOR":return "ADMINISTRADOR";
                    case "ROLE_IMPERSONATOR": return "SOCIO";
                }
            }
        } catch (Exception ignored) { }
        return null;
    }
}

package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.reciclame.demo.dto.SearchResponseDTO;
import org.reciclame.demo.dto.UsuarioSesion;
import org.reciclame.demo.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint REST de búsqueda global consumido por el search box del topbar.
 *
 * <p>El filtrado por tipo (normativa / estudio / usuario) se hace server-side
 * en {@link SearchService}, según el rol efectivo del usuario en sesión.</p>
 */
@RestController
@RequestMapping("/api/search")
public class SearchRestController {

    private static final Logger log = LoggerFactory.getLogger(SearchRestController.class);

    private final SearchService searchService;

    public SearchRestController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponseDTO buscar(@RequestParam(value = "q", required = false) String query,
                                    HttpSession session) {
        String rolEfectivo = resolverRolEfectivo(session);
        log.info("[SEARCH] q='{}', rol='{}'", query, rolEfectivo);
        SearchResponseDTO resp = searchService.buscar(query, rolEfectivo);
        log.info("[SEARCH] resultados={}, tiposPermitidos={}", resp.getResultados().size(), resp.getTiposPermitidos());
        return resp;
    }

    private String resolverRolEfectivo(HttpSession session) {
        if (session == null) return "";
        Object attr = session.getAttribute("usuarioSesion");
        if (attr instanceof UsuarioSesion us) {
            return us.getRolNombre() != null ? us.getRolNombre() : "";
        }
        return "";
    }
}

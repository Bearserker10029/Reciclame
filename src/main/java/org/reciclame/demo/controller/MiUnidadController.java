package org.reciclame.demo.controller;

import org.reciclame.demo.dto.MiUnidadCarpetaArbolNodo;
import org.reciclame.demo.dto.MiUnidadContenidoFila;
import org.reciclame.demo.dto.MiUnidadUltimoAcceso;
import org.reciclame.demo.model.MiUnidadCarpeta;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.service.DocumentosRecientesService;
import org.reciclame.demo.service.MiUnidadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/socio/mi-unidad")
public class MiUnidadController {

    private final MiUnidadService miUnidadService;
    private final DocumentosRecientesService documentosRecientesService;

    public MiUnidadController(MiUnidadService miUnidadService,
                              DocumentosRecientesService documentosRecientesService) {
        this.miUnidadService = miUnidadService;
        this.documentosRecientesService = documentosRecientesService;
    }

    private void attrSocio(Model model, UserDetails user) {
        model.addAttribute("rol", "SOCIO");
        model.addAttribute("rolNombre", "Socio");
        model.addAttribute("notifUrl", "/socio/notificaciones");
    }

    @GetMapping
    public String vista(@RequestParam(required = false) Long carpeta,
                       Model model,
                       @AuthenticationPrincipal UserDetails user) {
        attrSocio(model, user);
        Usuario u = miUnidadService.requireUsuario(user.getUsername());
        Long usuarioId = u.getId();

        MiUnidadCarpeta carpetaActual = null;
        if (carpeta != null) {
            carpetaActual = miUnidadService.findCarpetaIfOwned(usuarioId, carpeta).orElse(null);
            if (carpetaActual == null) {
                model.addAttribute("error", "Carpeta no encontrada.");
            }
        }

        Long carpetaId = carpetaActual != null ? carpetaActual.getId() : null;
        Long carpetaPadreId = null;
        if (carpetaActual != null && carpetaActual.getParent() != null) {
            carpetaPadreId = carpetaActual.getParent().getId();
        }
        List<MiUnidadCarpeta> subcarpetas = miUnidadService.listarSubcarpetas(usuarioId, carpetaId);
        List<MiUnidadItem> items = miUnidadService.listarItems(usuarioId, carpetaId);

        List<MiUnidadContenidoFila> contenidoFilas = miUnidadService.listarFilasContenido(
                usuarioId, carpetaId, subcarpetas, items);

        model.addAttribute("carpetaActual", carpetaActual);
        model.addAttribute("carpetaId", carpetaId);
        model.addAttribute("carpetaPadreId", carpetaPadreId);
        model.addAttribute("breadcrumb", miUnidadService.breadcrumb(usuarioId, carpetaId));
        model.addAttribute("subcarpetas", subcarpetas);
        model.addAttribute("contenidoFilas", contenidoFilas);

        List<MiUnidadUltimoAcceso> ultimosAccesos = documentosRecientesService.listarUltimos(usuarioId, 4);
        model.addAttribute("ultimosAccesos", ultimosAccesos);

        return "Socio/mi-unidad";
    }

    @PostMapping("/registro-acceso")
    @ResponseBody
    public ResponseEntity<Void> registroAcceso(@RequestParam String documentoTipo,
                                               @RequestParam Long documentoId,
                                               @AuthenticationPrincipal UserDetails user) {
        Usuario u = miUnidadService.requireUsuario(user.getUsername());
        documentosRecientesService.registrarVista(u.getId(), documentoTipo, documentoId);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/api/carpetas-arbol", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MiUnidadCarpetaArbolNodo apiCarpetasArbol(@AuthenticationPrincipal UserDetails user) {
        Usuario u = miUnidadService.requireUsuario(user.getUsername());
        return miUnidadService.arbolCarpetasUsuario(u.getId());
    }

    @PostMapping("/carpeta/crear")
    public String crearCarpeta(@RequestParam(required = false) Long parentId,
                                @RequestParam String nombre,
                                RedirectAttributes ra,
                                @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            String solicitado = nombre == null ? "" : nombre.trim();
            var creada = miUnidadService.crearCarpeta(u.getId(), parentId, nombre);
            if (!solicitado.equalsIgnoreCase(creada.getNombre())) {
                ra.addFlashAttribute("ok",
                        "Carpeta creada como «" + creada.getNombre() + "» (ya existía otra con el mismo nombre).");
            } else {
                ra.addFlashAttribute("ok", "Carpeta creada.");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(parentId);
    }

    @PostMapping("/carpeta/eliminar")
    public String eliminarCarpeta(@RequestParam Long id,
                                  @RequestParam(required = false) Long parentId,
                                  RedirectAttributes ra,
                                  @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            miUnidadService.eliminarCarpeta(u.getId(), id);
            ra.addFlashAttribute("ok", "Carpeta eliminada.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(parentId);
    }

    /**
     * Añadir documento a Mi Unidad sin redirección (Normativo, Estudios, etc.).
     */
    @PostMapping(value = "/api/item/agregar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiAgregarItem(
            @RequestParam(required = false) Long carpetaId,
            @RequestParam String documentoTipo,
            @RequestParam Long documentoId,
            @RequestParam(required = false) String documentoTitulo,
            @AuthenticationPrincipal UserDetails user) {
        Map<String, Object> body = new HashMap<>();
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            miUnidadService.agregarDocumento(u.getId(), carpetaId, documentoTipo, documentoId, documentoTitulo);
            body.put("ok", true);
            body.put("message", "Se añadió correctamente.");
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            body.put("ok", false);
            body.put("message", ex.getMessage() != null ? ex.getMessage() : "No se pudo añadir.");
            return ResponseEntity.badRequest().body(body);
        }
    }

    @PostMapping("/item/agregar")
    public String agregar(@RequestParam(required = false) Long carpetaId,
                         @RequestParam String documentoTipo,
                         @RequestParam Long documentoId,
                         @RequestParam(required = false) String documentoTitulo,
                         RedirectAttributes ra,
                         @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            miUnidadService.agregarDocumento(u.getId(), carpetaId, documentoTipo, documentoId, documentoTitulo);
            ra.addFlashAttribute("ok", "Documento agregado a Mi Unidad.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(carpetaId);
    }

    @PostMapping("/item/quitar")
    public String quitar(@RequestParam Long itemId,
                        @RequestParam(required = false) Long carpetaId,
                        RedirectAttributes ra,
                        @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            miUnidadService.quitarItem(u.getId(), itemId);
            ra.addFlashAttribute("ok", "Documento quitado de Mi Unidad.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(carpetaId);
    }

    @PostMapping("/item/mover")
    public String mover(@RequestParam Long itemId,
                       @RequestParam(required = false) Long carpetaId,
                       @RequestParam(required = false) String nuevaCarpetaId,
                       RedirectAttributes ra,
                       @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            miUnidadService.moverItem(u.getId(), itemId, parseCarpetaIdOpcional(nuevaCarpetaId));
            ra.addFlashAttribute("ok", "Documento movido.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(carpetaId);
    }

    @PostMapping("/carpeta/mover")
    public String moverCarpeta(@RequestParam Long carpetaMovidaId,
                               @RequestParam(required = false) Long carpetaId,
                               @RequestParam(required = false) String nuevaCarpetaId,
                               RedirectAttributes ra,
                               @AuthenticationPrincipal UserDetails user) {
        try {
            Usuario u = miUnidadService.requireUsuario(user.getUsername());
            String nombreAntes = miUnidadService.findCarpetaIfOwned(u.getId(), carpetaMovidaId)
                    .map(c -> c.getNombre())
                    .orElse("");
            var movida = miUnidadService.moverCarpeta(u.getId(), carpetaMovidaId, parseCarpetaIdOpcional(nuevaCarpetaId));
            if (!nombreAntes.isEmpty() && !nombreAntes.equals(movida.getNombre())) {
                ra.addFlashAttribute("ok",
                        "Carpeta movida como «" + movida.getNombre() + "» (ya existía otra con el mismo nombre en el destino).");
            } else {
                ra.addFlashAttribute("ok", "Carpeta movida.");
            }
        } catch (Exception ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTrasCarpeta(carpetaId);
    }

    private static Long parseCarpetaIdOpcional(String raw) {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return Long.parseLong(raw.trim());
    }

    private static String redirectTrasCarpeta(Long carpetaId) {
        if (carpetaId == null) {
            return "redirect:/socio/mi-unidad";
        }
        return "redirect:/socio/mi-unidad?carpeta=" + carpetaId;
    }

}

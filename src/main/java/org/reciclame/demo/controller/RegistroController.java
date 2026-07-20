package org.reciclame.demo.controller;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.SolicitudesRegistro;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import org.reciclame.demo.service.ApiPeruDevService;
import org.reciclame.demo.service.EmailNotificationService;
import org.reciclame.demo.service.RegistroService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

// Dos formas de llegar a "pedir acceso": la página dedicada /registro (form
// urlencoded, procesarFormulario) y el panel embebido en el login (JSON,
// procesarJson — no pide "empresa" el form clásico, pero el panel del login
// sí). Ambas caen en el mismo guardar().
@Controller
public class RegistroController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RegistroController.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final SolicitudesRegistroRepository repository;
    private final RegistroService registroService;
    private final EmailNotificationService emailService;
    private final ApiPeruDevService apiPeruDevService;

    public RegistroController(SolicitudesRegistroRepository repository,
                              RegistroService registroService,
                              EmailNotificationService emailService,
                              ApiPeruDevService apiPeruDevService) {
        this.repository = repository;
        this.registroService = registroService;
        this.emailService = emailService;
        this.apiPeruDevService = apiPeruDevService;
    }

    @GetMapping("/registro/consulta-dni/{dni}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarDni(@PathVariable String dni) {
        return apiPeruDevService.consultarNombrePorDni(dni)
                .map(nombre -> ResponseEntity.ok(Map.<String, Object>of("encontrado", true, "nombreCompleto", nombre)))
                .orElseGet(() -> ResponseEntity.ok(Map.of("encontrado", false)));
    }


    @GetMapping("/registro")
    public String mostrarFormulario(Model model) {
        model.addAttribute("error", "");
        return "registro";
    }

    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String procesarFormulario(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(defaultValue = "") String dni,
            @RequestParam(name = "correo", defaultValue = "") String correo,
            @RequestParam(name = "aceptaTerminos", defaultValue = "false") boolean aceptaTerminos,
            Model model) {

        String empresa = "";

        if (!aceptaTerminos) {
            return error(model, nombre, dni, correo, empresa, "Debes aceptar los Términos y Condiciones para registrarte");
        }

        if (correo.isEmpty()) {
            return error(model, nombre, dni, correo, empresa, "El correo corporativo es requerido");
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            String msg = correo.contains("@")
                    ? "Ingresa un correo electrónico válido"
                    : "Ingresa un correo electrónico válido";
            return error(model, nombre, dni, correo, empresa, msg);
        }
        if (nombre.isBlank() || dni.isBlank()) {
            return error(model, nombre, dni, correo, empresa, "Todos los campos son requeridos");
        }

        if (!registroService.canRegister(correo, dni)) {
            String msg = registroService.getStatusMessage(correo, dni);
            return error(model, nombre, dni, correo, empresa,
                    msg.isEmpty() ? "Ya existe una solicitud o cuenta con este correo" : msg);
        }

        guardar(nombre, dni, correo, empresa);
        return "redirect:/registro/pendiente";
    }

    @GetMapping("/registro/pendiente")
    public String pendiente() {
        return "pendiente-registro";
    }


    @GetMapping("/legal/terminos")
    public String legalTerminos() {
        return "legal/terminos";
    }

    @GetMapping("/legal/cookies")
    public String legalCookies() {
        return "legal/cookies";
    }

    @GetMapping("/legal/privacidad")
    public String legalPrivacidad() {
        return "legal/privacidad";
    }

    @GetMapping("/legal/nosotros")
    public String legalNosotros() {
        return "legal/nosotros";
    }


    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> procesarJson(@RequestBody Map<String, String> body) {
        String nombre  = body.getOrDefault("nombre", "").trim();
        String dni     = body.getOrDefault("dni", "").trim();
        String correo  = body.getOrDefault("correo", "").trim();
        String empresa = body.getOrDefault("empresa", "").trim();

        if (correo.isEmpty()) return bad("El correo corporativo es requerido");
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return bad(correo.contains("@")
                    ? "Ingresa un correo electrónico válido"
                    : "Ingresa un correo electrónico válido");
        }
        if (nombre.isBlank() || dni.isBlank() || empresa.isBlank())
            return bad("Todos los campos son requeridos");

        if (!registroService.canRegister(correo, dni)) {
            String msg = registroService.getStatusMessage(correo, dni);
            return bad(msg.isEmpty() ? "Ya existe una solicitud o cuenta con este correo" : msg);
        }

        guardar(nombre, dni, correo, empresa);
        return ResponseEntity.ok(Map.of("success", true));
    }


    private void guardar(String nombre, String dni, String correo, String empresa) {
        SolicitudesRegistro s = new SolicitudesRegistro();
        s.setNombreCompleto(nombre);
        s.setDni(dni);
        s.setCorreoCorporativo(correo);
        s.setEmpresaNombre(empresa != null && !empresa.isBlank() ? empresa : null);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setEstadoActivo(true);
        s.setCreatedAt(Instant.now());
        repository.save(s);
        // La solicitud ya quedó guardada; que falle el correo de bienvenida
        // (SMTP caído, etc.) no debería tumbarle el registro al usuario.
        try {
            emailService.enviarBienvenida(correo, nombre);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo de bienvenida a {}: {}", correo, ex.getMessage());
        }
    }

    private String error(Model model, String nombre, String dni,
                         String correo, String empresa, String msg) {
        model.addAttribute("error", msg);
        model.addAttribute("nombre", nombre);
        model.addAttribute("dni", dni);
        model.addAttribute("correo", correo);
        model.addAttribute("empresa", empresa);
        return "registro";
    }

    private ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", msg));
    }
}

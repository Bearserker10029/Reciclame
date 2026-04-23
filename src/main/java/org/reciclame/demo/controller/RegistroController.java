package org.reciclame.demo.controller;

import org.reciclame.demo.model.EstadoSolicitud;
import org.reciclame.demo.model.SolicitudesRegistro;
import org.reciclame.demo.repository.SolicitudesRegistroRepository;
import org.reciclame.demo.service.RegistroService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

@Controller
public class RegistroController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@reciclame\\.org$");

    private final SolicitudesRegistroRepository repository;
    private final RegistroService registroService;

    public RegistroController(SolicitudesRegistroRepository repository,
                              RegistroService registroService) {
        this.repository = repository;
        this.registroService = registroService;
    }

    // ── Dedicated registration page ───────────────────────────

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
            @RequestParam(defaultValue = "") String empresa,
            Model model) {

        if (correo.isEmpty()) {
            return error(model, nombre, dni, correo, empresa, "El correo corporativo es requerido");
        }
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            String msg = correo.contains("@")
                    ? "Solo se aceptan correos corporativos (@reciclame.org)"
                    : "Ingresa un correo válido (ejemplo@reciclame.org)";
            return error(model, nombre, dni, correo, empresa, msg);
        }
        if (nombre.isBlank() || dni.isBlank() || empresa.isBlank()) {
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

    // ── JSON endpoint — keeps AC1 (login.html panel) working ──

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
                    ? "Solo se aceptan correos corporativos (@reciclame.org)"
                    : "Ingresa un correo válido (ejemplo@reciclame.org)");
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

    // ── Helpers ───────────────────────────────────────────────

    private void guardar(String nombre, String dni, String correo, String empresa) {
        SolicitudesRegistro s = new SolicitudesRegistro();
        s.setNombreCompleto(nombre);
        s.setDni(dni);
        s.setCorreoCorporativo(correo);
        s.setEmpresaNombre(empresa);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setEstadoActivo(true);
        s.setCreatedAt(Instant.now());
        repository.save(s);
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

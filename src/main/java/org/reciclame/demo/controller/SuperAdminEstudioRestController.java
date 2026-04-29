package org.reciclame.demo.controller;

import jakarta.validation.Valid;
import org.reciclame.demo.dto.EstudioRequestDTO;
import org.reciclame.demo.dto.EstudioResponseDTO;
import org.reciclame.demo.service.EstudioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller aislado para el rol SUPERADMIN.
 * Mapea /superadmin/api/estudios — cubierto por la regla
 * .requestMatchers("/superadmin/**").hasRole("SUPERADMIN") en SecurityConfig.
 * Reutiliza los mismos servicios de negocio que el Admin.
 */
@RestController
@RequestMapping("/superadmin/api/estudios")
public class SuperAdminEstudioRestController {

    private final EstudioService estudioService;

    public SuperAdminEstudioRestController(EstudioService estudioService) {
        this.estudioService = estudioService;
    }

    // ── READ ALL ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<EstudioResponseDTO>> listar() {
        return ResponseEntity.ok(estudioService.listarEstudios());
    }

    // ── READ ONE ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(estudioService.obtenerEstudioPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("estudio") @Valid EstudioRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            EstudioResponseDTO resultado = estudioService.crearEstudio(
                    dto, archivoUrl, archivoTipoMime, user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── UPDATE ────────────────────────────────────────────────
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestPart("estudio") @Valid EstudioRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            EstudioResponseDTO resultado = estudioService.actualizarEstudio(
                    id, dto, archivoUrl, archivoTipoMime);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── DELETE lógico ─────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            estudioService.eliminarEstudio(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Stub subida (reemplazar con S3/GCP en producción) ─────
    private String simularSubida(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        return "https://storage.googleapis.com/reciclame-docs/"
                + System.currentTimeMillis() + "_" + nombre;
    }
}

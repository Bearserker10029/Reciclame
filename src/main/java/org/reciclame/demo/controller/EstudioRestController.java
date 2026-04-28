package org.reciclame.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@RestController
@RequestMapping("/api/estudios")
public class EstudioRestController {

    private final EstudioService estudioService;
    private final ObjectMapper   objectMapper;

    public EstudioRestController(EstudioService estudioService) {
        this.estudioService = estudioService;
        this.objectMapper   = new ObjectMapper().registerModule(new JavaTimeModule());
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("estudio") String estudioJson,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user) {
        try {
            EstudioRequestDTO dto = objectMapper.readValue(estudioJson, EstudioRequestDTO.class);

            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }

            EstudioResponseDTO resultado = estudioService.crearEstudio(dto, archivoUrl, archivoTipoMime, user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── UPDATE ────────────────────────────────────────────────
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestPart("estudio") String estudioJson,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            EstudioRequestDTO dto = objectMapper.readValue(estudioJson, EstudioRequestDTO.class);

            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }

            EstudioResponseDTO resultado = estudioService.actualizarEstudio(id, dto, archivoUrl, archivoTipoMime);
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

    // ── Stub de subida de archivo (sustituir por S3/GCP) ──────
    private String simularSubida(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        return "https://storage.googleapis.com/reciclame-docs/" + System.currentTimeMillis() + "_" + nombre;
    }
}

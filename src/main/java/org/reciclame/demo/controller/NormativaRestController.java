package org.reciclame.demo.controller;

import jakarta.validation.Valid;
import org.reciclame.demo.dto.NormativaRequestDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.service.NormativaService;
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
@RequestMapping("/api/normativas")
public class NormativaRestController {

    private final NormativaService normativaService;

    public NormativaRestController(NormativaService normativaService) {
        this.normativaService = normativaService;
    }

    // ── READ ALL ──────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<NormativaResponseDTO>> listar() {
        return ResponseEntity.ok(normativaService.listarNormativas());
    }

    // ── READ ONE ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(normativaService.obtenerNormativaPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── CREATE ────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crear(
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo,
            @AuthenticationPrincipal UserDetails user) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            NormativaResponseDTO resultado = normativaService.crearNormativa(
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
            @RequestPart("normativa") @Valid NormativaRequestDTO dto,
            @RequestPart(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String archivoUrl      = null;
            String archivoTipoMime = null;
            if (archivo != null && !archivo.isEmpty()) {
                archivoUrl      = simularSubida(archivo);
                archivoTipoMime = archivo.getContentType();
            }
            NormativaResponseDTO resultado = normativaService.actualizarNormativa(
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
            normativaService.eliminarNormativa(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Stub subida (reemplazar con S3/GCP) ───────────────────
    private String simularSubida(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "documento";
        return "https://storage.googleapis.com/reciclame-docs/"
                + System.currentTimeMillis() + "_" + nombre;
    }
}

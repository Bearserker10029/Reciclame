package org.reciclame.demo.controller;

import org.reciclame.demo.dto.*;
import org.reciclame.demo.service.EtiquetaService;
import org.reciclame.demo.service.EstadosNormativaService;
import org.reciclame.demo.service.TematicaService;
import org.reciclame.demo.service.TiposNormativaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DiccionariosRestController {

    private final TematicaService         tematicaService;
    private final EtiquetaService         etiquetaService;
    private final TiposNormativaService   tiposNormativaService;
    private final EstadosNormativaService estadosNormativaService;

    public DiccionariosRestController(TematicaService tematicaService,
                                       EtiquetaService etiquetaService,
                                       TiposNormativaService tiposNormativaService,
                                       EstadosNormativaService estadosNormativaService) {
        this.tematicaService         = tematicaService;
        this.etiquetaService         = etiquetaService;
        this.tiposNormativaService   = tiposNormativaService;
        this.estadosNormativaService = estadosNormativaService;
    }

    // ══════════════════════════════════════════════════════════════
    //  TEMÁTICAS
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/tematicas")
    public ResponseEntity<List<TematicaResponseDTO>> listarTematicas() {
        return ResponseEntity.ok(tematicaService.listarTematicasTodas());
    }

    @PostMapping("/tematicas")
    public ResponseEntity<?> crearTematica(@RequestBody TematicaRequestDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(tematicaService.crearTematica(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tematicas/{id}")
    public ResponseEntity<?> actualizarTematica(@PathVariable Long id, @RequestBody TematicaRequestDTO dto) {
        try {
            return ResponseEntity.ok(tematicaService.actualizarTematica(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tematicas/{id}")
    public ResponseEntity<?> eliminarTematica(@PathVariable Long id) {
        try {
            tematicaService.eliminarTematica(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ETIQUETAS
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/etiquetas")
    public ResponseEntity<List<EtiquetaResponseDTO>> listarEtiquetas() {
        return ResponseEntity.ok(etiquetaService.listarEtiquetasTodas());
    }

    @PostMapping("/etiquetas")
    public ResponseEntity<?> crearEtiqueta(@RequestBody EtiquetaRequestDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(etiquetaService.crearEtiqueta(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/etiquetas/{id}")
    public ResponseEntity<?> actualizarEtiqueta(@PathVariable Long id, @RequestBody EtiquetaRequestDTO dto) {
        try {
            return ResponseEntity.ok(etiquetaService.actualizarEtiqueta(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/etiquetas/{id}")
    public ResponseEntity<?> eliminarEtiqueta(@PathVariable Long id) {
        try {
            etiquetaService.eliminarEtiqueta(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  TIPOS DE NORMATIVA
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/tipos-normativa")
    public ResponseEntity<List<TiposNormativaResponseDTO>> listarTipos() {
        return ResponseEntity.ok(tiposNormativaService.listarTiposTodos());
    }

    @PostMapping("/tipos-normativa")
    public ResponseEntity<?> crearTipo(@RequestBody TiposNormativaRequestDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(tiposNormativaService.crearTipo(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/tipos-normativa/{id}")
    public ResponseEntity<?> actualizarTipo(@PathVariable Long id, @RequestBody TiposNormativaRequestDTO dto) {
        try {
            return ResponseEntity.ok(tiposNormativaService.actualizarTipo(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/tipos-normativa/{id}")
    public ResponseEntity<?> eliminarTipo(@PathVariable Long id) {
        try {
            tiposNormativaService.eliminarTipo(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ESTADOS DE NORMATIVA
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/estados-normativa")
    public ResponseEntity<List<EstadosNormativaResponseDTO>> listarEstados() {
        return ResponseEntity.ok(estadosNormativaService.listarEstadosTodos());
    }

    @PostMapping("/estados-normativa")
    public ResponseEntity<?> crearEstado(@RequestBody EstadosNormativaRequestDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(estadosNormativaService.crearEstado(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/estados-normativa/{id}")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody EstadosNormativaRequestDTO dto) {
        try {
            return ResponseEntity.ok(estadosNormativaService.actualizarEstado(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/estados-normativa/{id}")
    public ResponseEntity<?> eliminarEstado(@PathVariable Long id) {
        try {
            estadosNormativaService.eliminarEstado(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

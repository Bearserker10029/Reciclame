package org.reciclame.demo.controller;

import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    // ── LISTAR TODAS ─────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Auditoria>> listarAuditorias() {
        return ResponseEntity.ok(auditoriaService.listarAuditorias());
    }

    // ── LISTAR CON PAGINACIÓN ────────────────────────────────────────
    @GetMapping("/pagina")
    public ResponseEntity<Page<Auditoria>> listarAuditoriasPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPaginado(page, size));
    }
    // ── LISTAR POR USUARIO ───────────────────────────────────────────
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Auditoria>> listarPorUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorUsuario(usuarioId));
    }

    @GetMapping("/usuario/{usuarioId}/pagina")
    public ResponseEntity<Page<Auditoria>> listarPorUsuarioPaginado(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorUsuarioPaginado(usuarioId, page, size));
    }

    // ── LISTAR POR MÓDULO ────────────────────────────────────────────
    @GetMapping("/modulo/{modulo}")
    public ResponseEntity<List<Auditoria>> listarPorModulo(@PathVariable String modulo) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorModulo(modulo));
    }

    @GetMapping("/modulo/{modulo}/pagina")
    public ResponseEntity<Page<Auditoria>> listarPorModuloPaginado(
            @PathVariable String modulo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorModuloPaginado(modulo, page, size));
    }

    // ── LISTAR POR ACCIÓN ────────────────────────────────────────────
    @GetMapping("/accion/{accion}")
    public ResponseEntity<List<Auditoria>> listarPorAccion(@PathVariable String accion) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorAccion(accion));
    }

    @GetMapping("/accion/{accion}/pagina")
    public ResponseEntity<Page<Auditoria>> listarPorAccionPaginado(
            @PathVariable String accion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPorAccionPaginado(accion, page, size));
    }

    // ── OBTENER POR ID ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Auditoria> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerAuditoriaPorId(id));
    }

    // ── MANEJO DE ERRORES ────────────────────────────────────────────
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
}

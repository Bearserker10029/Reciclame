package org.reciclame.demo.controller;

import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.service.AuditoriaHashChainService;
import org.reciclame.demo.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// API de solo lectura sobre la tabla de auditoría (logins, descargas,
// cambios). Cada registro va firmado con un hash encadenado al anterior
// (ver AuditoriaHashChainService), así que si alguien edita o borra una fila
// directo en la BD, /verificar-integridad lo detecta.
@RestController
@RequestMapping("/api/auditoria")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','SUPERADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<List<Auditoria>> listarAuditorias() {
        return ResponseEntity.ok(auditoriaService.listarAuditorias());
    }

    @GetMapping("/pagina")
    public ResponseEntity<Page<Auditoria>> listarAuditoriasPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditoriaService.listarAuditoriasPaginado(page, size));
    }
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

    @GetMapping("/{id}")
    public ResponseEntity<Auditoria> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerAuditoriaPorId(id));
    }

    @GetMapping("/verificar-integridad")
    public ResponseEntity<Map<String, Object>> verificarIntegridad() {
        AuditoriaHashChainService.ResultadoVerificacion r =
                auditoriaService.verificarIntegridadCadena();
        Map<String, Object> body = new HashMap<>();
        body.put("integro", r.isIntegro());
        body.put("totalRegistros", r.getTotalRegistros());
        body.put("registrosVerificados", r.getRegistrosVerificados());
        body.put("registrosSinFirma", r.getRegistrosSinFirma());
        body.put("idFalla", r.getIdFalla());
        body.put("hashEsperado", r.getHashEsperado());
        body.put("hashAnteriorEncontrado", r.getHashAnteriorEncontrado());
        body.put("hashActualEncontrado", r.getHashActualEncontrado());
        body.put("mensaje", r.getMensaje());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/verificar-integridad/rango")
    public ResponseEntity<Map<String, Object>> verificarIntegridadRango(
            @RequestParam String desde,
            @RequestParam String hasta) {
        Instant d = Instant.parse(desde);
        Instant h = Instant.parse(hasta);
        AuditoriaHashChainService.ResultadoVerificacion r =
                auditoriaService.verificarIntegridadCadenaEnRango(d, h);
        Map<String, Object> body = new HashMap<>();
        body.put("integro", r.isIntegro());
        body.put("totalRegistros", r.getTotalRegistros());
        body.put("registrosVerificados", r.getRegistrosVerificados());
        body.put("registrosSinFirma", r.getRegistrosSinFirma());
        body.put("idFalla", r.getIdFalla());
        body.put("hashEsperado", r.getHashEsperado());
        body.put("hashAnteriorEncontrado", r.getHashAnteriorEncontrado());
        body.put("hashActualEncontrado", r.getHashActualEncontrado());
        body.put("mensaje", r.getMensaje());
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }
}

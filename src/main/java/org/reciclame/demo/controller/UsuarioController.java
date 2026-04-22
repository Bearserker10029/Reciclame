package org.reciclame.demo.controller;

import org.reciclame.demo.dto.UsuarioRequestDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // CREATE
    // POST /api/usuarios
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> crearUsuario(@RequestBody UsuarioRequestDTO dto) {
        UsuarioResponseDTO nuevo = usuarioService.crearUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // READ ALL
    // GET /api/usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    //  READ ONE
    // GET /api/usuarios/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    // UPDATE
    // PUT /api/usuarios/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @RequestBody UsuarioRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, dto));
    }

    // DELETE (lógico)
    // DELETE /api/usuarios/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    // APROBAR solicitud pendiente
    // POST /api/usuarios/{id}/aprobar?rolId=X&empresaId=Y
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<UsuarioResponseDTO> aprobarUsuario(
            @PathVariable Long id,
            @RequestParam(required = false) Long rolId,
            @RequestParam(required = false) Long empresaId) {
        return ResponseEntity.ok(usuarioService.aprobarUsuario(id, rolId, empresaId));
    }

    // RECHAZAR solicitud pendiente
    // POST /api/usuarios/{id}/rechazar
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<UsuarioResponseDTO> rechazarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.rechazarUsuario(id));
    }

    // BLOQUEAR / DESBLOQUEAR usuario activo
    // POST /api/usuarios/{id}/bloquear
    @PostMapping("/{id}/bloquear")
    public ResponseEntity<UsuarioResponseDTO> bloquearUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.toggleBloquear(id));
    }

    // EXPORTAR CSV
    // GET /api/usuarios/export.csv
    @GetMapping("/export.csv")
    public ResponseEntity<String> exportarCsv() {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"usuarios.csv\"")
                .body(usuarioService.exportarCsv());
    }
}

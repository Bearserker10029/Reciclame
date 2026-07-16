package org.reciclame.demo.service;

import org.reciclame.demo.model.DocumentoCompartido;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.DocumentoCompartidoRepository;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * "Compartir con personas" al estilo Google Drive para estudios y normativas.
 * No reemplaza la visibilidad por rol ya existente (todo socio ve todos los
 * documentos de su alcance); sirve para señalar explícitamente un documento
 * a otro usuario y notificarle.
 */
@Service
public class CompartirService {

    public static final String TIPO_ESTUDIO = "ESTUDIO";
    public static final String TIPO_NORMATIVA = "NORMATIVA";

    private final DocumentoCompartidoRepository compartidoRepository;
    private final EstudioRepository estudioRepository;
    private final NormativaRepository normativaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public CompartirService(DocumentoCompartidoRepository compartidoRepository,
                            EstudioRepository estudioRepository,
                            NormativaRepository normativaRepository,
                            UsuarioRepository usuarioRepository,
                            NotificacionService notificacionService) {
        this.compartidoRepository = compartidoRepository;
        this.estudioRepository = estudioRepository;
        this.normativaRepository = normativaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    private void validarTipo(String tipo) {
        if (!TIPO_ESTUDIO.equalsIgnoreCase(tipo) && !TIPO_NORMATIVA.equalsIgnoreCase(tipo)) {
            throw new IllegalArgumentException("Tipo de documento inválido: " + tipo);
        }
    }

    /** Título y propietario (quien subió el documento) para la cabecera del modal. */
    public Map<String, Object> obtenerInfoDocumento(String tipo, Long documentoId) {
        validarTipo(tipo);
        Map<String, Object> info = new LinkedHashMap<>();
        if (TIPO_ESTUDIO.equalsIgnoreCase(tipo)) {
            Estudio e = estudioRepository.findById(documentoId)
                    .orElseThrow(() -> new RuntimeException("Estudio no encontrado: " + documentoId));
            info.put("titulo", e.getTitulo());
            info.put("propietarioNombre", e.getSubidaPor() != null ? e.getSubidaPor().getNombreCompleto() : "Recíclame");
            info.put("propietarioCorreo", e.getSubidaPor() != null ? e.getSubidaPor().getCorreoCorporativo() : null);
        } else {
            Normativa n = normativaRepository.findById(documentoId)
                    .orElseThrow(() -> new RuntimeException("Normativa no encontrada: " + documentoId));
            info.put("titulo", n.getTitulo());
            info.put("propietarioNombre", n.getCreadaPor() != null ? n.getCreadaPor().getNombreCompleto() : "Recíclame");
            info.put("propietarioCorreo", n.getCreadaPor() != null ? n.getCreadaPor().getCorreoCorporativo() : null);
        }
        return info;
    }

    /** Personas con acceso señalado (sin contar al propietario). */
    public List<Map<String, Object>> listarPersonas(String tipo, Long documentoId) {
        validarTipo(tipo);
        List<DocumentoCompartido> registros = compartidoRepository.findByDocumento(tipo.toUpperCase(), documentoId);
        return registros.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            Usuario u = r.getUsuario();
            m.put("usuarioId", u.getId());
            m.put("nombreCompleto", u.getNombreCompleto());
            m.put("correo", u.getCorreoCorporativo());
            m.put("compartidoEl", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            return m;
        }).toList();
    }

    /**
     * Comparte el documento con el usuario del correo indicado y le notifica.
     * Lanza IllegalArgumentException si no existe ningún usuario con ese correo
     * (a diferencia de Drive, este sistema solo comparte entre cuentas registradas).
     */
    @Transactional
    public Map<String, Object> agregarPersona(String tipo, Long documentoId, String correo,
                                              Long compartidoPorId, String enlace) {
        validarTipo(tipo);
        String correoNormalizado = correo == null ? "" : correo.trim().toLowerCase();
        Usuario destinatario = usuarioRepository.findByCorreoCorporativo(correoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ningún usuario de Recíclame con el correo: " + correo));

        if (destinatario.getId().equals(compartidoPorId)) {
            throw new IllegalArgumentException("No puedes compartir el documento contigo mismo");
        }

        String tipoUpper = tipo.toUpperCase();
        Optional<DocumentoCompartido> existente = compartidoRepository
                .findByTipoDocumentoAndDocumentoIdAndUsuarioId(tipoUpper, documentoId, destinatario.getId());
        if (existente.isPresent()) {
            throw new IllegalArgumentException(destinatario.getNombreCompleto() + " ya tiene acceso a este documento");
        }

        Usuario compartidoPor = usuarioRepository.findById(compartidoPorId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + compartidoPorId));

        DocumentoCompartido dc = new DocumentoCompartido();
        dc.setTipoDocumento(tipoUpper);
        dc.setDocumentoId(documentoId);
        dc.setUsuario(destinatario);
        dc.setCompartidoPor(compartidoPor);
        dc.setCreatedAt(Instant.now());
        compartidoRepository.save(dc);

        String titulo = (String) obtenerInfoDocumento(tipo, documentoId).get("titulo");
        try {
            notificacionService.notificarDocumentoCompartido(
                    destinatario, compartidoPor.getNombreCompleto(), titulo, enlace);
        } catch (Exception ex) { /* no rollback si falla la notificación */ }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("usuarioId", destinatario.getId());
        m.put("nombreCompleto", destinatario.getNombreCompleto());
        m.put("correo", destinatario.getCorreoCorporativo());
        return m;
    }

    @Transactional
    public void quitarPersona(String tipo, Long documentoId, Long usuarioId) {
        validarTipo(tipo);
        compartidoRepository.deleteByTipoDocumentoAndDocumentoIdAndUsuarioId(tipo.toUpperCase(), documentoId, usuarioId);
    }
}

package org.reciclame.demo.service;

import org.reciclame.demo.dto.MiUnidadUltimoAcceso;
import org.reciclame.demo.model.DocumentosReciente;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.DocumentosRecienteRepository;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DocumentosRecientesService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");

    private final DocumentosRecienteRepository recienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final MiUnidadService miUnidadService;

    public DocumentosRecientesService(DocumentosRecienteRepository recienteRepository,
                                      UsuarioRepository usuarioRepository,
                                      NormativaRepository normativaRepository,
                                      EstudioRepository estudioRepository,
                                      MiUnidadService miUnidadService) {
        this.recienteRepository = recienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.miUnidadService = miUnidadService;
    }

    @Transactional
    public void registrarVista(Long usuarioId, String documentoTipo, Long documentoId) {
        if (usuarioId == null || documentoId == null || documentoTipo == null) {
            return;
        }
        String tipo = normalizarTipo(documentoTipo);
        if (tipo == null) {
            return;
        }
        Instant ahora = Instant.now();
        Optional<DocumentosReciente> existente = recienteRepository
                .findByUsuario_IdAndDocumentoTipoAndDocumentoId(usuarioId, tipo, documentoId);
        if (existente.isPresent()) {
            DocumentosReciente r = existente.get();
            r.setUltimaVista(ahora);
            recienteRepository.save(r);
            return;
        }
        Usuario u = usuarioRepository.findById(usuarioId).orElse(null);
        if (u == null) {
            return;
        }
        DocumentosReciente n = new DocumentosReciente();
        n.setUsuario(u);
        n.setDocumentoTipo(tipo);
        n.setDocumentoId(documentoId);
        n.setUltimaVista(ahora);
        recienteRepository.save(n);
    }

    public long contarDocumentosVistos(Long usuarioId) {
        if (usuarioId == null) return 0L;
        return recienteRepository.countByUsuario_Id(usuarioId);
    }

    public List<MiUnidadUltimoAcceso> listarUltimos(Long usuarioId, int max) {
        return listarUltimos(usuarioId, max, "socio");
    }

    /**
     * @param rolBase prefijo de ruta para las tarjetas ("socio" o "visualizador").
     *                Antes estaba hardcodeado a "/socio/...", así que las tarjetas
     *                de "vistos recientemente" de un VISUALIZADOR apuntaban a rutas
     *                que ese rol no puede usar (403/404 al hacer clic).
     */
    public List<MiUnidadUltimoAcceso> listarUltimos(Long usuarioId, int max, String rolBase) {
        if (usuarioId == null || max <= 0) {
            return List.of();
        }
        List<DocumentosReciente> filas = recienteRepository
                .findByUsuario_IdOrderByUltimaVistaDesc(usuarioId, PageRequest.of(0, max * 2));
        Instant ahora = Instant.now();
        List<MiUnidadUltimoAcceso> out = new ArrayList<>();
        for (DocumentosReciente dr : filas) {
            if (out.size() >= max) {
                break;
            }
            construirTarjeta(dr, usuarioId, ahora, rolBase).ifPresent(out::add);
        }
        return out;
    }

    private Optional<MiUnidadUltimoAcceso> construirTarjeta(DocumentosReciente dr, Long usuarioId, Instant ahora, String rolBase) {
        String tipo = dr.getDocumentoTipo();
        Long docId = dr.getDocumentoId();
        // "visualizador" no tiene módulo Mi Unidad, así que no aplica el ?from=mi-unidad.
        String sufijo = "socio".equals(rolBase) ? "/ver?from=mi-unidad" : "/ver";
        if (MiUnidadItem.TIPO_NORMATIVA.equals(tipo)) {
            return normativaRepository.findById(docId).map(n -> new MiUnidadUltimoAcceso(
                    tipo,
                    docId,
                    miUnidadService.tituloParaTarjetaAccesoReciente(usuarioId, tipo, docId, n.getTitulo()),
                    tiempoRelativo(dr.getUltimaVista(), ahora),
                    "/" + rolBase + "/normativo/" + docId + sufijo,
                    urlDescarga(n.getArchivoUrl(), n.getTitulo()),
                    iconVariantDesdeUrl(n.getArchivoUrl())
            ));
        }
        if (MiUnidadItem.TIPO_ESTUDIO.equals(tipo)) {
            return estudioRepository.findById(docId).map(e -> new MiUnidadUltimoAcceso(
                    tipo,
                    docId,
                    miUnidadService.tituloParaTarjetaAccesoReciente(usuarioId, tipo, docId, e.getTitulo()),
                    tiempoRelativo(dr.getUltimaVista(), ahora),
                    "/" + rolBase + "/estudios/" + docId + sufijo,
                    urlDescarga(e.getArchivoUrl(), e.getTitulo()),
                    iconVariantDesdeUrl(e.getArchivoUrl())
            ));
        }
        return Optional.empty();
    }

    private String urlDescarga(String archivoUrl, String titulo) {
        if (archivoUrl == null || archivoUrl.isBlank()) {
            return "#";
        }
        return "/api/documentos/descargar?url=" + URLEncoder.encode(archivoUrl, StandardCharsets.UTF_8)
                + "&titulo=" + URLEncoder.encode(titulo != null ? titulo : "", StandardCharsets.UTF_8);
    }

    private static String normalizarTipo(String raw) {
        if (raw == null) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MiUnidadItem.TIPO_NORMATIVA.equals(u) || MiUnidadItem.TIPO_ESTUDIO.equals(u)) {
            return u;
        }
        return null;
    }

    static String tiempoRelativo(Instant cuando, Instant ahora) {
        if (cuando == null) {
            return "";
        }
        long seg = Math.max(0, Duration.between(cuando, ahora).getSeconds());
        if (seg < 45) {
            return "hace un momento";
        }
        long min = seg / 60;
        if (min < 60) {
            return min <= 1 ? "hace 1 min" : "hace " + min + " min";
        }
        long h = min / 60;
        if (h < 24) {
            return h == 1 ? "hace 1 h" : "hace " + h + " h";
        }
        long d = h / 24;
        if (d < 7) {
            return d == 1 ? "ayer" : "hace " + d + " días";
        }
        return java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", new Locale.Builder().setLanguage("es").setRegion("PE").build())
                .withZone(ZONA)
                .format(cuando);
    }

    private static String iconVariantDesdeUrl(String archivoUrl) {
        if (archivoUrl == null || archivoUrl.isBlank()) {
            return "generic";
        }
        String path = archivoUrl.contains("?") ? archivoUrl.substring(0, archivoUrl.indexOf('?')) : archivoUrl;
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot >= path.length() - 1) {
            return "generic";
        }
        String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "doc";
            case "xls", "xlsx", "csv" -> "sheet";
            case "ppt", "pptx" -> "slide";
            default -> "generic";
        };
    }
}

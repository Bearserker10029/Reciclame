package org.reciclame.demo.service;

import org.reciclame.demo.dto.MiUnidadCarpetaArbolNodo;
import org.reciclame.demo.dto.MiUnidadContenidoFila;
import org.reciclame.demo.model.Estudio;
import org.reciclame.demo.model.MiUnidadCarpeta;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.model.Normativa;
import org.reciclame.demo.model.Usuario;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.MiUnidadCarpetaRepository;
import org.reciclame.demo.repository.MiUnidadItemRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.reciclame.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MiUnidadService {

    private final MiUnidadCarpetaRepository carpetaRepository;
    private final MiUnidadItemRepository itemRepository;
    private final UsuarioRepository usuarioRepository;
    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;

    public MiUnidadService(MiUnidadCarpetaRepository carpetaRepository,
                          MiUnidadItemRepository itemRepository,
                          UsuarioRepository usuarioRepository,
                          NormativaRepository normativaRepository,
                          EstudioRepository estudioRepository) {
        this.carpetaRepository = carpetaRepository;
        this.itemRepository = itemRepository;
        this.usuarioRepository = usuarioRepository;
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
    }

    public Usuario requireUsuario(String correo) {
        return usuarioRepository.findByCorreoCorporativo(correo)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    public Optional<MiUnidadCarpeta> findCarpetaIfOwned(Long usuarioId, Long carpetaId) {
        if (carpetaId == null) {
            return Optional.empty();
        }
        return carpetaRepository.findByIdAndUsuario_Id(carpetaId, usuarioId);
    }

    public List<MiUnidadCarpeta> listarSubcarpetas(Long usuarioId, Long parentId) {
        if (parentId == null) {
            return carpetaRepository.findByUsuario_IdAndParentIsNullOrderByNombreAsc(usuarioId);
        }
        return carpetaRepository.findByUsuario_IdAndParent_IdOrderByNombreAsc(usuarioId, parentId);
    }

    public List<MiUnidadItem> listarItems(Long usuarioId, Long carpetaId) {
        if (carpetaId == null) {
            return itemRepository.findByUsuario_IdAndCarpetaIsNullOrderByCreatedAtDesc(usuarioId);
        }
        return itemRepository.findByUsuario_IdAndCarpeta_IdOrderByCreatedAtDesc(usuarioId, carpetaId);
    }

    public String tituloDocumento(String tipo, Long documentoId) {
        if (MiUnidadItem.TIPO_NORMATIVA.equals(tipo)) {
            return normativaRepository.findById(documentoId)
                    .map(n -> n.getTitulo())
                    .orElse("Normativa no disponible");
        }
        if (MiUnidadItem.TIPO_ESTUDIO.equals(tipo)) {
            return estudioRepository.findById(documentoId)
                    .map(e -> e.getTitulo())
                    .orElse("Estudio no disponible");
        }
        return "Documento";
    }

    /** Título en listados: preferir el guardado al añadir; si no, resolver desde el repositorio. */
    public String tituloParaMostrar(MiUnidadItem item) {
        String snap = item.getTituloPresentacion();
        if (snap != null) {
            String t = snap.trim();
            if (!t.isEmpty()) {
                return t;
            }
        }
        return tituloDocumento(item.getDocumentoTipo(), item.getDocumentoId());
    }

    /**
     * Título en “Últimos accesos”: alinear con la tabla de Mi Unidad cuando el documento está en alguna carpeta del usuario.
     */
    public String tituloParaTarjetaAccesoReciente(Long usuarioId, String documentoTipo, Long documentoId,
                                                  String tituloEntidad) {
        return itemRepository.findFirstByUsuario_IdAndDocumentoTipoAndDocumentoIdOrderByCreatedAtDesc(
                        usuarioId, documentoTipo, documentoId)
                .map(this::tituloParaMostrar)
                .orElseGet(() -> (tituloEntidad != null && !tituloEntidad.isBlank())
                        ? tituloEntidad
                        : tituloDocumento(documentoTipo, documentoId));
    }

    private static String normalizarTituloPresentacion(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > 500 ? t.substring(0, 500) : t;
    }

    /** Árbol de carpetas del usuario para submenú "Añadir a" (cada nodo incluye hijos ordenados por nombre). */
    @Transactional(readOnly = true)
    public MiUnidadCarpetaArbolNodo arbolCarpetasUsuario(Long usuarioId) {
        List<MiUnidadCarpeta> todas = carpetaRepository.findByUsuario_IdOrderByNombreAsc(usuarioId);
        Map<Long, List<MiUnidadCarpeta>> porPadre = new HashMap<>();
        for (MiUnidadCarpeta c : todas) {
            Long pid = c.getParent() != null ? c.getParent().getId() : null;
            porPadre.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }
        Comparator<MiUnidadCarpeta> porNombre = Comparator.comparing(MiUnidadCarpeta::getNombre, String.CASE_INSENSITIVE_ORDER);
        for (List<MiUnidadCarpeta> lista : porPadre.values()) {
            lista.sort(porNombre);
        }
        List<MiUnidadCarpetaArbolNodo> hijosRaiz = construirNodosArbolJerarquia(null, porPadre);
        return new MiUnidadCarpetaArbolNodo(null, "Mi Unidad", hijosRaiz);
    }

    private List<MiUnidadCarpetaArbolNodo> construirNodosArbolJerarquia(Long parentId,
                                                                        Map<Long, List<MiUnidadCarpeta>> porPadre) {
        List<MiUnidadCarpeta> lista = porPadre.getOrDefault(parentId, List.of());
        List<MiUnidadCarpetaArbolNodo> out = new ArrayList<>();
        for (MiUnidadCarpeta c : lista) {
            List<MiUnidadCarpetaArbolNodo> sub = construirNodosArbolJerarquia(c.getId(), porPadre);
            out.add(new MiUnidadCarpetaArbolNodo(c.getId(), c.getNombre(), sub));
        }
        return out;
    }

    /** Carpetas con ruta tipo /A/B para desplegable de mover. */
    public Map<Long, String> carpetasConRuta(Long usuarioId) {
        List<MiUnidadCarpeta> todas = carpetaRepository.findByUsuario_IdOrderByNombreAsc(usuarioId);
        Map<Long, MiUnidadCarpeta> porId = new LinkedHashMap<>();
        for (MiUnidadCarpeta c : todas) {
            porId.put(c.getId(), c);
        }
        Map<Long, String> resultado = new LinkedHashMap<>();
        for (MiUnidadCarpeta c : todas) {
            resultado.put(c.getId(), construirRuta(c, porId));
        }
        return resultado;
    }

    private String construirRuta(MiUnidadCarpeta c, Map<Long, MiUnidadCarpeta> porId) {
        List<String> partes = new ArrayList<>();
        MiUnidadCarpeta cur = c;
        int guard = 0;
        while (cur != null && guard++ < 64) {
            partes.addFirst(cur.getNombre());
            Long pid = cur.getParent() != null ? cur.getParent().getId() : null;
            cur = pid != null ? porId.get(pid) : null;
        }
        return "/" + String.join("/", partes);
    }

    public List<BreadcrumbEntry> breadcrumb(Long usuarioId, Long carpetaId) {
        List<BreadcrumbEntry> list = new ArrayList<>();
        list.add(new BreadcrumbEntry(null, "Mi Unidad"));
        if (carpetaId == null) {
            return list;
        }
        List<BreadcrumbEntry> chain = new ArrayList<>();
        MiUnidadCarpeta cur = carpetaRepository.findByIdAndUsuario_Id(carpetaId, usuarioId).orElse(null);
        int guard = 0;
        while (cur != null && guard++ < 64) {
            chain.add(new BreadcrumbEntry(cur.getId(), cur.getNombre()));
            if (cur.getParent() == null) {
                break;
            }
            Long pid = cur.getParent().getId();
            cur = carpetaRepository.findByIdAndUsuario_Id(pid, usuarioId).orElse(null);
        }
        java.util.Collections.reverse(chain);
        list.addAll(chain);
        return list;
    }

    @Transactional
    public MiUnidadCarpeta crearCarpeta(Long usuarioId, Long parentId, String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty()) {
            throw new IllegalArgumentException("El nombre de la carpeta es obligatorio");
        }
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalStateException("Usuario no válido"));
        MiUnidadCarpeta carpeta = new MiUnidadCarpeta();
        carpeta.setUsuario(u);
        carpeta.setNombre(n);
        carpeta.setSortOrder(0);
        if (parentId != null) {
            MiUnidadCarpeta parent = carpetaRepository.findByIdAndUsuario_Id(parentId, usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta padre no encontrada"));
            carpeta.setParent(parent);
        }
        return carpetaRepository.save(carpeta);
    }

    @Transactional
    public void eliminarCarpeta(Long usuarioId, Long carpetaId) {
        MiUnidadCarpeta c = carpetaRepository.findByIdAndUsuario_Id(carpetaId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));
        eliminarCarpetaRecursivo(usuarioId, c.getId());
    }

    private void eliminarCarpetaRecursivo(Long usuarioId, Long carpetaId) {
        List<MiUnidadCarpeta> hijos = carpetaRepository.findByUsuario_IdAndParent_IdOrderByNombreAsc(usuarioId, carpetaId);
        for (MiUnidadCarpeta h : hijos) {
            eliminarCarpetaRecursivo(usuarioId, h.getId());
        }
        itemRepository.deleteByCarpeta_IdAndUsuario_Id(carpetaId, usuarioId);
        MiUnidadCarpeta c = carpetaRepository.findByIdAndUsuario_Id(carpetaId, usuarioId)
                .orElseThrow();
        carpetaRepository.delete(c);
    }

    @Transactional
    public void agregarDocumento(Long usuarioId, Long carpetaId, String documentoTipo, Long documentoId,
                                 String tituloPresentacion) {
        if (documentoId == null || documentoId <= 0) {
            throw new IllegalArgumentException("ID de documento no válido");
        }
        String tipo = normalizarTipo(documentoTipo);
        if (MiUnidadItem.TIPO_NORMATIVA.equals(tipo)) {
            Normativa n = normativaRepository.findById(documentoId)
                    .orElseThrow(() -> new IllegalArgumentException("Normativa no encontrada"));
            // Misma regla que el listado del repositorio (activas): el socio puede añadir lo que ve en catálogo.
            if (!Boolean.TRUE.equals(n.getEstadoActivo())) {
                throw new IllegalArgumentException("Esta normativa no está activa y no se puede añadir.");
            }
        } else {
            Estudio e = estudioRepository.findById(documentoId)
                    .orElseThrow(() -> new IllegalArgumentException("Estudio no encontrado"));
            if (!Boolean.TRUE.equals(e.getEstadoActivo())) {
                throw new IllegalArgumentException("Este estudio no está activo y no se puede añadir.");
            }
        }
        MiUnidadCarpeta carpeta = null;
        if (carpetaId != null) {
            carpeta = carpetaRepository.findByIdAndUsuario_Id(carpetaId, usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada"));
        }
        boolean dup = carpeta == null
                ? itemRepository.existsByUsuario_IdAndCarpetaIsNullAndDocumentoTipoAndDocumentoId(usuarioId, tipo, documentoId)
                : itemRepository.existsByUsuario_IdAndCarpeta_IdAndDocumentoTipoAndDocumentoId(usuarioId, carpeta.getId(), tipo, documentoId);
        if (dup) {
            throw new IllegalArgumentException("Este documento ya está en esta ubicación");
        }
        Usuario u = usuarioRepository.findById(usuarioId).orElseThrow();
        MiUnidadItem item = new MiUnidadItem();
        item.setUsuario(u);
        item.setCarpeta(carpeta);
        item.setDocumentoTipo(tipo);
        item.setDocumentoId(documentoId);
        item.setTituloPresentacion(normalizarTituloPresentacion(tituloPresentacion));
        itemRepository.save(item);
    }

    @Transactional
    public void quitarItem(Long usuarioId, Long itemId) {
        MiUnidadItem it = itemRepository.findByIdAndUsuario_Id(itemId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento no encontrado"));
        itemRepository.delete(it);
    }

    @Transactional
    public void moverItem(Long usuarioId, Long itemId, Long nuevaCarpetaId) {
        MiUnidadItem it = itemRepository.findByIdAndUsuario_Id(itemId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Elemento no encontrado"));
        MiUnidadCarpeta destino = null;
        if (nuevaCarpetaId != null) {
            destino = carpetaRepository.findByIdAndUsuario_Id(nuevaCarpetaId, usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Carpeta destino no encontrada"));
        }
        Long destId = destino == null ? null : destino.getId();
        Long curCarpetaId = it.getCarpeta() == null ? null : it.getCarpeta().getId();
        if (java.util.Objects.equals(curCarpetaId, destId)) {
            return;
        }
        boolean dup = destino == null
                ? itemRepository.existsByUsuario_IdAndCarpetaIsNullAndDocumentoTipoAndDocumentoId(
                        usuarioId, it.getDocumentoTipo(), it.getDocumentoId())
                : itemRepository.existsByUsuario_IdAndCarpeta_IdAndDocumentoTipoAndDocumentoId(
                        usuarioId, destino.getId(), it.getDocumentoTipo(), it.getDocumentoId());
        if (dup) {
            throw new IllegalArgumentException("Ya existe este documento en la carpeta destino");
        }
        it.setCarpeta(destino);
        itemRepository.save(it);
    }

    private static String normalizarTipo(String raw) {
        if (raw == null) return MiUnidadItem.TIPO_NORMATIVA;
        String u = raw.trim().toUpperCase();
        if (MiUnidadItem.TIPO_ESTUDIO.equals(u)) return MiUnidadItem.TIPO_ESTUDIO;
        return MiUnidadItem.TIPO_NORMATIVA;
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("d MMM yyyy", new Locale.Builder().setLanguage("es").setRegion("ES").build());

    public int contarContenidoDirectoEnCarpeta(Long usuarioId, Long carpetaId) {
        long sub = carpetaRepository.countByUsuario_IdAndParent_Id(usuarioId, carpetaId);
        long docs = itemRepository.countByUsuario_IdAndCarpeta_Id(usuarioId, carpetaId);
        return (int) (sub + docs);
    }

    public List<MiUnidadContenidoFila> listarFilasContenido(Long usuarioId, Long carpetaId,
                                                            List<MiUnidadCarpeta> subcarpetas,
                                                            List<MiUnidadItem> items) {
        List<MiUnidadContenidoFila> out = new ArrayList<>();
        for (MiUnidadCarpeta c : subcarpetas) {
            out.add(filaCarpeta(c, usuarioId));
        }
        for (MiUnidadItem it : items) {
            out.add(filaDocumento(it));
        }
        return out;
    }

    public MiUnidadContenidoFila filaCarpeta(MiUnidadCarpeta c, Long usuarioId) {
        int n = contarContenidoDirectoEnCarpeta(usuarioId, c.getId());
        String tamano = n == 0 ? "Carpeta vacía" : n + " elemento" + (n == 1 ? "" : "s");
        Instant inst = c.getCreatedAt();
        long ms = inst != null ? inst.toEpochMilli() : 0L;
        String fecha = inst != null ? formatearFecha(inst) : "—";
        return new MiUnidadContenidoFila(
                true,
                c.getId(),
                null,
                c.getNombre(),
                "Recíclame",
                fecha,
                tamano,
                null,
                null,
                ms,
                nombreOrden(c.getNombre())
        );
    }

    public MiUnidadContenidoFila filaDocumento(MiUnidadItem it) {
        String titulo = tituloParaMostrar(it);
        String tipo = it.getDocumentoTipo();
        Long docId = it.getDocumentoId();
        String prop;
        String fechaStr;
        String tamano;
        long fechaMs;

        if (MiUnidadItem.TIPO_NORMATIVA.equals(tipo)) {
            Optional<Normativa> opt = normativaRepository.findById(docId);
            if (opt.isPresent()) {
                Normativa n = opt.get();
                prop = propietarioNormativa(n);
                LocalDate pub = fechaPublicacionNormativa(n);
                fechaStr = pub != null ? formatearFecha(pub) : formatearFecha(n.getCreatedAt());
                fechaMs = fechaMillis(pub, n.getCreatedAt());
                tamano = etiquetaTamanoDoc(n.getArchivoTipoMime(), "Normativa");
            } else {
                prop = "Recíclame";
                fechaStr = formatearFecha(it.getCreatedAt());
                fechaMs = it.getCreatedAt().toEpochMilli();
                tamano = "Normativa (referencia)";
            }
        } else {
            Optional<Estudio> opt = estudioRepository.findById(docId);
            if (opt.isPresent()) {
                Estudio e = opt.get();
                prop = propietarioEstudio(e);
                LocalDate pub = fechaPublicacionEstudio(e);
                fechaStr = pub != null ? formatearFecha(pub) : formatearFecha(e.getCreatedAt());
                fechaMs = fechaMillis(pub, e.getCreatedAt());
                tamano = etiquetaTamanoDoc(e.getArchivoTipoMime(), "Estudio");
            } else {
                prop = "Recíclame";
                fechaStr = formatearFecha(it.getCreatedAt());
                fechaMs = it.getCreatedAt().toEpochMilli();
                tamano = "Estudio (referencia)";
            }
        }

        return new MiUnidadContenidoFila(
                false,
                null,
                it.getId(),
                titulo,
                prop,
                fechaStr,
                tamano,
                tipo,
                docId,
                fechaMs,
                nombreOrden(titulo)
        );
    }

    private static String nombreOrden(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String formatearFecha(Instant i) {
        if (i == null) {
            return "—";
        }
        return i.atZone(ZoneId.systemDefault()).toLocalDate().format(FMT_FECHA);
    }

    private static String formatearFecha(LocalDate d) {
        if (d == null) {
            return "—";
        }
        return d.format(FMT_FECHA);
    }

    private static long fechaMillis(LocalDate d, Instant fallback) {
        if (d != null) {
            return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return fallback != null ? fallback.toEpochMilli() : 0L;
    }

    private static LocalDate fechaPublicacionNormativa(Normativa n) {
        if (n.getFechaPromulgacion() != null) {
            return n.getFechaPromulgacion();
        }
        if (n.getFechaEntradaVigencia() != null) {
            return n.getFechaEntradaVigencia();
        }
        if (n.getAnio() != null) {
            return LocalDate.of(n.getAnio(), 1, 1);
        }
        return null;
    }

    private static LocalDate fechaPublicacionEstudio(Estudio e) {
        if (e.getFechaEstudio() != null) {
            return e.getFechaEstudio();
        }
        if (e.getAnio() != null) {
            return LocalDate.of(e.getAnio(), 1, 1);
        }
        return null;
    }

    private static String propietarioNormativa(Normativa n) {
        String tema = n.getTematica() != null ? n.getTematica().getNombre() : null;
        if (tema != null && !tema.isBlank()) {
            return "Recíclame · " + tema.trim();
        }
        return "Recíclame";
    }

    private static String propietarioEstudio(Estudio e) {
        if (e.getAutor() != null && !e.getAutor().isBlank()) {
            return "Recíclame · " + e.getAutor().trim();
        }
        String tema = e.getTematica() != null ? e.getTematica().getNombre() : null;
        if (tema != null && !tema.isBlank()) {
            return "Recíclame · " + tema.trim();
        }
        return "Recíclame";
    }

    private static String etiquetaTamanoDoc(String mime, String fallbackTipo) {
        if (mime == null || mime.isBlank()) {
            return fallbackTipo + " · referencia";
        }
        String m = mime.toLowerCase(Locale.ROOT);
        if (m.contains("pdf")) {
            return "PDF · referencia";
        }
        if (m.contains("presentation") || m.contains("powerpoint")) {
            return "PPTX · referencia";
        }
        if (m.contains("word") || m.contains("msword")) {
            return "DOC · referencia";
        }
        return (mime.length() > 28 ? mime.substring(0, 25) + "…" : mime) + " · ref.";
    }

    public record BreadcrumbEntry(Long carpetaId, String nombre) {}
}

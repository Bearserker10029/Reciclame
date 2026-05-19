package org.reciclame.demo.service;

import org.reciclame.demo.dto.NotificacionResponseDTO;
import org.reciclame.demo.dto.SocioReporteActividadFila;
import org.reciclame.demo.dto.SocioReporteMiUnidadFila;
import org.reciclame.demo.dto.SocioReporteNotifFila;
import org.reciclame.demo.dto.SocioReportesView;
import org.reciclame.demo.model.DocumentosReciente;
import org.reciclame.demo.model.MiUnidadItem;
import org.reciclame.demo.repository.DocumentosRecienteRepository;
import org.reciclame.demo.repository.EstudioRepository;
import org.reciclame.demo.repository.MiUnidadCarpetaRepository;
import org.reciclame.demo.repository.MiUnidadItemRepository;
import org.reciclame.demo.repository.NormativaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SocioReportesService {

    private static final ZoneId ZONA = ZoneId.of("America/Lima");
    private static final Locale LOCALE_PE = new Locale.Builder().setLanguage("es").setRegion("PE").build();
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZONA);
    private static final DateTimeFormatter FMT_SOLO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONA);

    private final DocumentosRecienteRepository recienteRepository;
    private final MiUnidadItemRepository itemRepository;
    private final MiUnidadCarpetaRepository carpetaRepository;
    private final NormativaRepository normativaRepository;
    private final EstudioRepository estudioRepository;
    private final MiUnidadService miUnidadService;
    private final NotificacionService notificacionService;

    public SocioReportesService(DocumentosRecienteRepository recienteRepository,
                                MiUnidadItemRepository itemRepository,
                                MiUnidadCarpetaRepository carpetaRepository,
                                NormativaRepository normativaRepository,
                                EstudioRepository estudioRepository,
                                MiUnidadService miUnidadService,
                                NotificacionService notificacionService) {
        this.recienteRepository = recienteRepository;
        this.itemRepository = itemRepository;
        this.carpetaRepository = carpetaRepository;
        this.normativaRepository = normativaRepository;
        this.estudioRepository = estudioRepository;
        this.miUnidadService = miUnidadService;
        this.notificacionService = notificacionService;
    }

    @Transactional(readOnly = true)
    public SocioReportesView construir(Long usuarioId) {
        if (usuarioId == null) {
            return vistaVacia();
        }

        Instant ahora = Instant.now();
        LocalDate hoy = LocalDate.ofInstant(ahora, ZONA);
        YearMonth ym = YearMonth.from(hoy);
        Instant inicioMes = ym.atDay(1).atStartOfDay(ZONA).toInstant();

        long docMes = recienteRepository.countByUsuario_IdAndUltimaVistaGreaterThanEqual(usuarioId, inicioMes);
        long totalRecientes = recienteRepository.countByUsuario_Id(usuarioId);
        long nItems = itemRepository.countByUsuario_Id(usuarioId);
        long nCarpetas = carpetaRepository.countByUsuario_Id(usuarioId);

        List<NotificacionResponseDTO> notifs = notificacionService.listarPorUsuario(usuarioId);
        long leidas = notifs.stream().filter(n -> Boolean.TRUE.equals(n.getLeida())).count();

        List<DocumentosReciente> recientesPagina = recienteRepository
                .findByUsuario_IdOrderByUltimaVistaDesc(usuarioId, PageRequest.of(0, 25));
        List<DocumentosReciente> recientesChart = recienteRepository
                .findByUsuario_IdOrderByUltimaVistaDesc(usuarioId, PageRequest.of(0, 2000));

        List<String> etiquetasSemanas = new ArrayList<>();
        LocalDate lunes0 = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
        for (int i = 0; i < 8; i++) {
            LocalDate ini = lunes0.plusWeeks(i);
            LocalDate fin = ini.plusDays(6);
            etiquetasSemanas.add(
                    ini.format(DateTimeFormatter.ofPattern("d MMM", LOCALE_PE))
                            + " – "
                            + fin.format(DateTimeFormatter.ofPattern("d MMM", LOCALE_PE)));
        }
        int[] buckets = new int[8];
        for (DocumentosReciente dr : recientesChart) {
            if (dr.getUltimaVista() == null) {
                continue;
            }
            LocalDate d = LocalDate.ofInstant(dr.getUltimaVista(), ZONA);
            LocalDate lunesDoc = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            long w = ChronoUnit.WEEKS.between(lunes0, lunesDoc);
            if (w >= 0 && w < 8) {
                buckets[(int) w]++;
            }
        }
        List<Integer> valoresSemanas = new ArrayList<>(8);
        for (int v : buckets) {
            valoresSemanas.add(v);
        }

        long nNorm = recientesChart.stream().filter(r -> MiUnidadItem.TIPO_NORMATIVA.equals(r.getDocumentoTipo())).count();
        long nEst = recientesChart.stream().filter(r -> MiUnidadItem.TIPO_ESTUDIO.equals(r.getDocumentoTipo())).count();

        List<SocioReporteActividadFila> actividad = new ArrayList<>();
        for (DocumentosReciente dr : recientesPagina) {
            actividad.add(filaActividad(usuarioId, dr));
        }

        List<MiUnidadItem> items = itemRepository.findByUsuario_IdOrderByCreatedAtDesc(usuarioId, PageRequest.of(0, 50));
        Map<Long, String> rutas = miUnidadService.carpetasConRuta(usuarioId);
        List<SocioReporteMiUnidadFila> filasMu = new ArrayList<>();
        for (MiUnidadItem it : items) {
            String carpetaLbl = "Mi Unidad (raíz)";
            if (it.getCarpeta() != null && it.getCarpeta().getId() != null) {
                carpetaLbl = rutas.getOrDefault(it.getCarpeta().getId(), "/");
            }
            String tipoLbl = MiUnidadItem.TIPO_NORMATIVA.equals(it.getDocumentoTipo()) ? "Normativa" : "Estudio";
            String fecha = it.getCreatedAt() != null ? FMT_SOLO_FECHA.format(it.getCreatedAt()) : "—";
            filasMu.add(new SocioReporteMiUnidadFila(
                    carpetaLbl,
                    miUnidadService.tituloParaMostrar(it),
                    tipoLbl,
                    fecha
            ));
        }

        List<SocioReporteNotifFila> filasNotif = new ArrayList<>();
        for (NotificacionResponseDTO n : notifs.stream().limit(20).toList()) {
            String fecha = n.getCreatedAt() != null ? FMT_SOLO_FECHA.format(n.getCreatedAt()) : "—";
            String est = Boolean.TRUE.equals(n.getLeida()) ? "Leída" : "Sin leer";
            filasNotif.add(new SocioReporteNotifFila(fecha, n.getTitulo() != null ? n.getTitulo() : "(Sin título)", est));
        }

        String hintMes = docMes > 0
                ? "Documentos con última vista registrada en " + ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, LOCALE_PE) + " " + ym.getYear()
                : "Sin vistas registradas este mes (abre documentos en Normativo o Estudios)";
        String hintMu = nItems > 0
                ? nCarpetas + " carpeta" + (nCarpetas != 1 ? "s" : "") + " · ordenado por más reciente"
                : "Aún no hay documentos en Mi Unidad";
        String hintNotif = notifs.isEmpty()
                ? "No hay notificaciones activas"
                : Math.round((100.0 * leidas) / notifs.size()) + "% leídas · total " + notifs.size();
        String hintDocs = totalRecientes > 0
                ? "Basado en tus últimos accesos registrados"
                : "Abre normativas o estudios para empezar el seguimiento";

        return SocioReportesView.builder()
                .documentosUltimaVistaEnMes((int) docMes)
                .itemsMiUnidad((int) nItems)
                .carpetasMiUnidad((int) nCarpetas)
                .notifLeidas((int) leidas)
                .notifTotal(notifs.size())
                .documentosDistintosSeguidos((int) totalRecientes)
                .kpiMesHint(hintMes)
                .kpiMiUnidadHint(hintMu)
                .kpiNotifHint(hintNotif)
                .kpiDocsHint(hintDocs)
                .actividad(actividad)
                .miUnidad(filasMu)
                .notificaciones(filasNotif)
                .chartSemanasEtiquetas(etiquetasSemanas)
                .chartSemanasValores(valoresSemanas)
                .donutNormativas((int) nNorm)
                .donutEstudios((int) nEst)
                .build();
    }

    private SocioReporteActividadFila filaActividad(Long usuarioId, DocumentosReciente dr) {
        String tipo = dr.getDocumentoTipo();
        Long docId = dr.getDocumentoId();
        String tipoLbl = MiUnidadItem.TIPO_NORMATIVA.equals(tipo) ? "Normativa" : "Estudio";
        String titulo;
        if (MiUnidadItem.TIPO_NORMATIVA.equals(tipo)) {
            titulo = normativaRepository.findById(docId)
                    .map(n -> miUnidadService.tituloParaTarjetaAccesoReciente(usuarioId, tipo, docId, n.getTitulo()))
                    .orElse("Normativa #" + docId);
        } else {
            titulo = estudioRepository.findById(docId)
                    .map(e -> miUnidadService.tituloParaTarjetaAccesoReciente(usuarioId, tipo, docId, e.getTitulo()))
                    .orElse("Estudio #" + docId);
        }
        String fh = dr.getUltimaVista() != null ? FMT_FECHA_HORA.format(dr.getUltimaVista()) : "—";
        return new SocioReporteActividadFila(fh, titulo, tipoLbl, "Última vista");
    }

    private SocioReportesView vistaVacia() {
        List<String> labels = new ArrayList<>();
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate lunes0 = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(7);
        for (int i = 0; i < 8; i++) {
            LocalDate ini = lunes0.plusWeeks(i);
            LocalDate fin = ini.plusDays(6);
            labels.add(ini.format(DateTimeFormatter.ofPattern("d MMM", LOCALE_PE))
                    + " – "
                    + fin.format(DateTimeFormatter.ofPattern("d MMM", LOCALE_PE)));
        }
        List<Integer> zeros = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            zeros.add(0);
        }
        return SocioReportesView.builder()
                .documentosUltimaVistaEnMes(0)
                .itemsMiUnidad(0)
                .carpetasMiUnidad(0)
                .notifLeidas(0)
                .notifTotal(0)
                .documentosDistintosSeguidos(0)
                .kpiMesHint("Sin datos de usuario")
                .kpiMiUnidadHint("—")
                .kpiNotifHint("—")
                .kpiDocsHint("—")
                .actividad(List.of())
                .miUnidad(List.of())
                .notificaciones(List.of())
                .chartSemanasEtiquetas(labels)
                .chartSemanasValores(zeros)
                .donutNormativas(0)
                .donutEstudios(0)
                .build();
    }
}

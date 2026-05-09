package org.reciclame.demo.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SocioReportesView {
    /** Documentos distintos con última vista en el mes calendario actual (zona Lima). */
    private final int documentosUltimaVistaEnMes;
    private final int itemsMiUnidad;
    private final int carpetasMiUnidad;
    private final int notifLeidas;
    private final int notifTotal;
    /** Filas en documentos_recientes del usuario. */
    private final int documentosDistintosSeguidos;
    private final String kpiMesHint;
    private final String kpiMiUnidadHint;
    private final String kpiNotifHint;
    private final String kpiDocsHint;
    private final List<SocioReporteActividadFila> actividad;
    private final List<SocioReporteMiUnidadFila> miUnidad;
    private final List<SocioReporteNotifFila> notificaciones;
    private final List<String> chartSemanasEtiquetas;
    private final List<Integer> chartSemanasValores;
    private final int donutNormativas;
    private final int donutEstudios;
}

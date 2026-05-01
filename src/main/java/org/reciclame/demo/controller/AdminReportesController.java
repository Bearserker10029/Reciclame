package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.ReporteService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AdminReportesController {

    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;
    private final NormativaService normativaService;
    private final ReporteService reporteService;

    public AdminReportesController(AuditoriaService auditoriaService,
                                   UsuarioService usuarioService,
                                   NormativaService normativaService,
                                   ReporteService reporteService) {
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
        this.normativaService = normativaService;
        this.reporteService = reporteService;
    }

    @GetMapping("/admin/reportes/export/auditoria.pdf")
    public void exportarAuditoriaPDF(HttpServletResponse response,
                                     @AuthenticationPrincipal UserDetails user) throws IOException {
        List<Auditoria> auditorias = auditoriaService.listarAuditorias();

        List<Map<String, ?>> data = auditorias.stream().<Map<String, ?>>map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("fechaAccion", a.getFechaAccion());
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuarioNombre", a.getUsuarioNombre());
            row.put("ipAddress", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).collect(Collectors.toList());

        Map<String, Integer> porModulo = new LinkedHashMap<>();
        auditorias.forEach(a -> porModulo.merge(a.getModulo() != null ? a.getModulo() : "Sin módulo", 1, Integer::sum));
        List<Map<String, ?>> chart1Data = new ArrayList<>();
        porModulo.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart1Data.add(m); });

        Map<String, Integer> porEstado = new LinkedHashMap<>();
        auditorias.forEach(a -> porEstado.merge(a.getEstado() != null ? a.getEstado() : "Sin estado", 1, Integer::sum));
        List<Map<String, ?>> chart2Data = new ArrayList<>();
        porEstado.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart2Data.add(m); });

        Map<String, Object> params = new HashMap<>();
        params.put("chartData1", new JRMapCollectionDataSource(chart1Data));
        params.put("chartData2", new JRMapCollectionDataSource(chart2Data));

        byte[] pdf = reporteService.generarReportePDF("auditoria_report", new JRMapCollectionDataSource(data), params);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/auditoria.xlsx")
    public void exportarAuditoriaXLSX(HttpServletResponse response,
                                      @AuthenticationPrincipal UserDetails user) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("America/Lima"));
        List<Auditoria> auditorias = auditoriaService.listarAuditorias();
        List<Map<String, Object>> datos = auditorias.stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", a.getFechaAccion() != null ? fmt.format(a.getFechaAccion()) : "");
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuario", a.getUsuarioNombre());
            row.put("ip", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).collect(Collectors.toList());

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Auditoría",
                "Registro de actividades en la plataforma",
                new String[]{"Fecha", "Accion", "Modulo", "Estado", "Usuario", "Ip", "Descripcion"}, datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/admin/reportes/export/usuarios.pdf")
    public void exportarUsuariosPDF(HttpServletResponse response,
                                    @AuthenticationPrincipal UserDetails user) throws IOException {
        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();

        List<Map<String, ?>> data = usuarios.stream().<Map<String, ?>>map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rolNombre", u.getRolNombre() != null ? u.getRolNombre() : "");
            row.put("empresaNombre", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "");
            row.put("estadoCuenta", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin());
            return row;
        }).collect(Collectors.toList());

        Map<String, Integer> porRol = new LinkedHashMap<>();
        usuarios.forEach(u -> porRol.merge(u.getRolNombre() != null ? u.getRolNombre() : "Sin rol", 1, Integer::sum));
        List<Map<String, ?>> chart1Data = new ArrayList<>();
        porRol.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart1Data.add(m); });

        Map<String, Integer> porEstado = new LinkedHashMap<>();
        usuarios.forEach(u -> porEstado.merge(u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "Sin estado", 1, Integer::sum));
        List<Map<String, ?>> chart2Data = new ArrayList<>();
        porEstado.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart2Data.add(m); });

        Map<String, Object> params = new HashMap<>();
        params.put("chartData1", new JRMapCollectionDataSource(chart1Data));
        params.put("chartData2", new JRMapCollectionDataSource(chart2Data));

        byte[] pdf = reporteService.generarReportePDF("usuarios_report", new JRMapCollectionDataSource(data), params);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/usuarios.xlsx")
    public void exportarUsuariosXLSX(HttpServletResponse response,
                                     @AuthenticationPrincipal UserDetails user) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("America/Lima"));
        List<UsuarioResponseDTO> usuarios = usuarioService.listarUsuarios();
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombre", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rol", u.getRolNombre() != null ? u.getRolNombre() : "");
            row.put("empresa", u.getEmpresaNombre() != null ? u.getEmpresaNombre() : "");
            row.put("estado", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin() != null ? fmt.format(u.getUltimoLogin()) : "");
            return row;
        }).collect(Collectors.toList());

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Usuarios",
                "Listado de usuarios de la plataforma",
                new String[]{"Nombre", "Dni", "Correo", "Cargo", "Rol", "Empresa", "Estado", "Ultimo Login"}, datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/admin/reportes/export/normativas.pdf")
    public void exportarNormativasPDF(HttpServletResponse response,
                                      @AuthenticationPrincipal UserDetails user) throws IOException {
        List<NormativaResponseDTO> normativas = normativaService.listarNormativas();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("America/Lima"));

        List<Map<String, ?>> data = normativas.stream().<Map<String, ?>>map(n -> {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", n.getTipoNormativaNombre() != null ? n.getTipoNormativaNombre() : "");
            row.put("tematicaNombre", n.getTematicaNombre() != null ? n.getTematicaNombre() : "");
            row.put("estado", n.getEstadoNormativaNombre() != null ? n.getEstadoNormativaNombre() : "");
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion", n.getCreatedAt() != null ? dateFmt.format(n.getCreatedAt()) : "");
            return row;
        }).collect(Collectors.toList());

        Map<String, Integer> porTematica = new LinkedHashMap<>();
        normativas.forEach(n -> porTematica.merge(n.getTematicaNombre() != null ? n.getTematicaNombre() : "Sin temática", 1, Integer::sum));
        List<Map<String, ?>> chart1Data = new ArrayList<>();
        porTematica.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart1Data.add(m); });

        Map<String, Integer> porTipo = new LinkedHashMap<>();
        normativas.forEach(n -> porTipo.merge(n.getTipoNormativaNombre() != null ? n.getTipoNormativaNombre() : "Sin tipo", 1, Integer::sum));
        List<Map<String, ?>> chart2Data = new ArrayList<>();
        porTipo.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("label", k); m.put("value", v); chart2Data.add(m); });

        Map<String, Integer> porAnio = new LinkedHashMap<>();
        normativas.forEach(n -> porAnio.merge(n.getAnio() != null ? String.valueOf(n.getAnio()) : "Sin año", 1, Integer::sum));
        List<Map<String, ?>> chart3Data = new ArrayList<>();
        porAnio.forEach((k, v) -> { Map<String, Object> m = new HashMap<>(); m.put("category", k); m.put("value", v); chart3Data.add(m); });

        Map<String, Object> params = new HashMap<>();
        params.put("chartData1", new JRMapCollectionDataSource(chart1Data));
        params.put("chartData2", new JRMapCollectionDataSource(chart2Data));
        params.put("chartData3", new JRMapCollectionDataSource(chart3Data));

        byte[] pdf = reporteService.generarReportePDF("normativas_estudios_report", new JRMapCollectionDataSource(data), params);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/normativas.xlsx")
    public void exportarNormativasXLSX(HttpServletResponse response,
                                       @AuthenticationPrincipal UserDetails user) throws IOException {
        List<NormativaResponseDTO> normativas = normativaService.listarNormativas();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = normativas.stream().map(n -> {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", n.getTipoNormativaNombre() != null ? n.getTipoNormativaNombre() : "");
            row.put("tematica", n.getTematicaNombre() != null ? n.getTematicaNombre() : "");
            row.put("estado", n.getEstadoNormativaNombre() != null ? n.getEstadoNormativaNombre() : "");
            row.put("ano", n.getAnio() != null ? String.valueOf(n.getAnio()) : "");
            row.put("descargas", n.getDescargasCount() != null ? String.valueOf(n.getDescargasCount()) : "0");
            row.put("fechaCreacion", n.getCreatedAt() != null ? dateFmt.format(n.getCreatedAt()) : "");
            return row;
        }).collect(Collectors.toList());

        byte[] xlsx = reporteService.generarReporteExcel("Reporte de Normativas",
                "Listado de normativas de la plataforma",
                new String[]{"Titulo", "Tipo", "Tematica", "Estado", "Ano", "Descargas", "Fecha Creacion"}, datos);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }
}

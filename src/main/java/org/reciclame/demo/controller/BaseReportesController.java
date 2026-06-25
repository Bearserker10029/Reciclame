package org.reciclame.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.reciclame.demo.dto.ChatbotConsultaFilaDTO;
import org.reciclame.demo.dto.NormativaResponseDTO;
import org.reciclame.demo.dto.UsuarioResponseDTO;
import org.reciclame.demo.model.Auditoria;
import org.reciclame.demo.service.AuditoriaService;
import org.reciclame.demo.service.ChatbotService;
import org.reciclame.demo.service.EmailNotificationService;
import org.reciclame.demo.service.EstudioService;
import org.reciclame.demo.service.NormativaService;
import org.reciclame.demo.service.ReporteService;
import org.reciclame.demo.service.UsuarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class BaseReportesController {

    protected abstract UsuarioService usuarioService();
    protected abstract NormativaService normativaService();
    protected abstract EstudioService estudioService();
    protected abstract ReporteService reporteService();
    protected abstract AuditoriaService auditoriaService();
    protected abstract ChatbotService chatbotService();
    protected abstract EmailNotificationService emailNotificationService();

    // ══════════════════════════════════════════════════════════════════════
    // Endpoint unificado de exportación de USUARIOS (descarga + correo)
    // ══════════════════════════════════════════════════════════════════════

    public static class ExportRequest {
        public String formato;     // "pdf" | "xlsx"
        public Integer limite;     // 0 = todos
        public String accion;      // "download" | "email" | "both"
    }

    public static class ExportResult {
        public boolean enviado;
        public boolean descarga;
        public int totalRegistros;
        public String mensaje;
        public String correo;
    }

    @PostMapping("/admin/reportes/export/usuarios")
    @org.springframework.web.bind.annotation.ResponseBody
    protected ResponseEntity<?> exportarUsuariosUnificado(
            @RequestBody ExportRequest req,
            @AuthenticationPrincipal UserDetails user) throws IOException {

        String formato = req.formato != null ? req.formato.toLowerCase() : "pdf";
        int limite = req.limite != null ? req.limite : 100;
        String accion = req.accion != null ? req.accion.toLowerCase() : "download";

        List<UsuarioResponseDTO> usuarios = usuarioService().listarUsuariosLimit(limite);
        byte[] archivo;
        String nombreArchivo;
        String mime;

        if ("xlsx".equals(formato)) {
            archivo = construirUsuariosXLSX(usuarios);
            nombreArchivo = "usuarios_report.xlsx";
            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            archivo = construirUsuariosPDF(usuarios);
            nombreArchivo = "usuarios_report.pdf";
            mime = "application/pdf";
        }

        // Si la acción incluye correo, envía
        if ("email".equals(accion) || "both".equals(accion)) {
            String correo = user != null ? user.getUsername() : null;
            String nombre = correo;
            try {
                var dto = usuarioService().findByCorreo(correo).orElse(null);
                if (dto != null && dto.getNombreCompleto() != null) nombre = dto.getNombreCompleto();
            } catch (Exception ignored) {}

            emailNotificationService().enviarReporteAdjunto(
                    correo, nombre, "Reporte de Usuarios",
                    usuarios.size(), archivo, nombreArchivo, mime);

            if ("email".equals(accion)) {
                ExportResult r = new ExportResult();
                r.enviado = true; r.descarga = false;
                r.totalRegistros = usuarios.size();
                r.correo = correo;
                r.mensaje = "Reporte enviado a " + correo;
                return ResponseEntity.ok(r);
            }
        }

        // Devuelve binario (download o both)
        return ResponseEntity.ok()
                .header("Content-Type", mime)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .header("X-Registros", String.valueOf(usuarios.size()))
                .header("X-Email-Sent", String.valueOf("both".equals(accion)))
                .body(archivo);
    }

    private byte[] construirUsuariosPDF(List<UsuarioResponseDTO> usuarios) {
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("empresaNombre", u.getEmpresaNombre());
            row.put("estadoCuenta", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin());
            return row;
        }).toList();

        Map<String, Integer> rolCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (UsuarioResponseDTO u : usuarios) {
            rolCount.merge(u.getRolNombre() != null ? u.getRolNombre() : "SIN_ROL", 1, Integer::sum);
            estadoCount.merge(u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO", 1, Integer::sum);
        }
        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map.Entry<String, Integer> e : rolCount.entrySet()) {
            Map<String, Object> m = new HashMap<>(); m.put("label", e.getKey()); m.put("value", e.getValue());
            rolData.add(m);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> e : estadoCount.entrySet()) {
            Map<String, Object> m = new HashMap<>(); m.put("label", e.getKey()); m.put("value", e.getValue());
            estadoData.add(m);
        }

        return reporteService().generarReportePDF("usuarios_report", datos, null,
                new JRMapCollectionDataSource(rolData),
                new JRMapCollectionDataSource(estadoData));
    }

    private byte[] construirUsuariosXLSX(List<UsuarioResponseDTO> usuarios) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rol", u.getRolNombre());
            row.put("empresa", u.getEmpresaNombre());
            row.put("estado", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin() != null ? fmt.format(u.getUltimoLogin()) : "");
            return row;
        }).toList();

        Map<String, Integer> rolCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (UsuarioResponseDTO u : usuarios) {
            rolCount.merge(u.getRolNombre() != null ? u.getRolNombre() : "SIN_ROL", 1, Integer::sum);
            estadoCount.merge(u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO", 1, Integer::sum);
        }
        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map.Entry<String, Integer> e : rolCount.entrySet()) {
            Map<String, Object> m = new HashMap<>(); m.put("label", e.getKey()); m.put("value", e.getValue());
            rolData.add(m);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> e : estadoCount.entrySet()) {
            Map<String, Object> m = new HashMap<>(); m.put("label", e.getKey()); m.put("value", e.getValue());
            estadoData.add(m);
        }

        return reporteService().generarReporteExcelConGraficos("Reporte de Usuarios",
                "Total: " + usuarios.size() + " registros",
                new String[] { "Nombre Completo", "DNI", "Correo Corporativo", "Cargo", "Rol", "Empresa", "Estado", "Último Login" },
                datos, rolData, estadoData);
    }

    @GetMapping("/admin/reportes/export/auditoria.pdf")
    protected void exportarAuditoriaPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Auditoria> auditorias = auditoriaService().listarAuditoriasUltimas100();
        List<Map<String, Object>> datos = auditorias.stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("fechaAccion", a.getFechaAccion());
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuarioNombre", a.getUsuarioNombre());
            row.put("ipAddress", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).toList();

        Map<String, Integer> accionesCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (Auditoria a : auditorias) {
            String accionKey = a.getAccion() != null ? a.getAccion() : "SIN_ACCION";
            accionesCount.put(accionKey, accionesCount.getOrDefault(accionKey, 0) + 1);
            String estadoKey = a.getEstado() != null ? a.getEstado() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> accionesData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : accionesCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            accionesData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(accionesData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(estadoData);

        byte[] pdf = reporteService().generarReportePDF("auditoria_report", datos, null, chartData1, chartData2);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/auditoria.xlsx")
    protected void exportarAuditoriaXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Auditoria> auditorias = auditoriaService().listarAuditoriasUltimas100();
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
        }).toList();

        Map<String, Integer> accionesCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (Auditoria a : auditorias) {
            String accionKey = a.getAccion() != null ? a.getAccion() : "SIN_ACCION";
            accionesCount.put(accionKey, accionesCount.getOrDefault(accionKey, 0) + 1);
            String estadoKey = a.getEstado() != null ? a.getEstado() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> accionesData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : accionesCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            accionesData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        byte[] xlsx = reporteService().generarReporteExcelConGraficos("Reporte de Auditoría",
                "Últimas 100 acciones registradas",
                new String[] { "Fecha", "Accion", "Modulo", "Estado", "Usuario", "IP", "Descripcion" },
                datos, accionesData, estadoData);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"auditoria_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/admin/reportes/export/usuarios.pdf")
    protected void exportarUsuariosPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        List<UsuarioResponseDTO> usuarios = usuarioService().listarUsuariosUltimos100();
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("empresaNombre", u.getEmpresaNombre());
            row.put("estadoCuenta", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin());
            return row;
        }).toList();

        Map<String, Integer> rolCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (UsuarioResponseDTO u : usuarios) {
            String rolKey = u.getRolNombre() != null ? u.getRolNombre() : "SIN_ROL";
            rolCount.put(rolKey, rolCount.getOrDefault(rolKey, 0) + 1);
            String estadoKey = u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : rolCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            rolData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(rolData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(estadoData);

        byte[] pdf = reporteService().generarReportePDF("usuarios_report", datos, null, chartData1, chartData2);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/usuarios.xlsx")
    protected void exportarUsuariosXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<UsuarioResponseDTO> usuarios = usuarioService().listarUsuariosUltimos100();
        List<Map<String, Object>> datos = usuarios.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("nombreCompleto", u.getNombreCompleto());
            row.put("dni", u.getDni());
            row.put("correoCorporativo", u.getCorreoCorporativo());
            row.put("cargo", u.getCargo());
            row.put("rol", u.getRolNombre());
            row.put("empresa", u.getEmpresaNombre());
            row.put("estado", u.getEstadoCuenta());
            row.put("ultimoLogin", u.getUltimoLogin() != null ? fmt.format(u.getUltimoLogin()) : "");
            return row;
        }).toList();

        Map<String, Integer> rolCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (UsuarioResponseDTO u : usuarios) {
            String rolKey = u.getRolNombre() != null ? u.getRolNombre() : "SIN_ROL";
            rolCount.put(rolKey, rolCount.getOrDefault(rolKey, 0) + 1);
            String estadoKey = u.getEstadoCuenta() != null ? u.getEstadoCuenta() : "SIN_ESTADO";
            estadoCount.put(estadoKey, estadoCount.getOrDefault(estadoKey, 0) + 1);
        }

        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : rolCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            rolData.add(row);
        }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : estadoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            estadoData.add(row);
        }

        byte[] xlsx = reporteService().generarReporteExcelConGraficos("Reporte de Usuarios",
                "Últimos 100 usuarios registrados",
                new String[] { "Nombre Completo", "DNI", "Correo Corporativo", "Cargo", "Rol", "Empresa", "Estado",
                        "Último Login" },
                datos, rolData, estadoData);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"usuarios_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    @GetMapping("/admin/reportes/export/normativas.pdf")
    protected void exportarNormativasPDF(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        List<Map<String, Object>> datos = new ArrayList<>();
        List<NormativaResponseDTO> normativas = normativaService().listarNormativasUltimas100();
        List<org.reciclame.demo.dto.EstudioResponseDTO> estudios = estudioService().listarEstudiosUltimos100();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (NormativaResponseDTO n : normativas) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre());
            row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    n.getCreatedAt() != null ? fmt.format(n.getCreatedAt().atZone(ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", e.getTitulo());
            row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", e.getEstadoActivo() != null && e.getEstadoActivo() ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    e.getCreatedAt() != null ? fmt.format(e.getCreatedAt().atZone(ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        Map<String, Integer> tipoCount = new LinkedHashMap<>();
        tipoCount.put("Normativa", normativas.size());
        tipoCount.put("Estudio", estudios.size());

        Map<String, Integer> tematicaCount = new LinkedHashMap<>();
        for (NormativaResponseDTO n : normativas) {
            String key = n.getTematicaNombre() != null ? n.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            String key = e.getTematicaNombre() != null ? e.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }

        Map<Integer, Integer> descargasPorAnio = new LinkedHashMap<>();
        for (NormativaResponseDTO n : normativas) {
            if (n.getAnio() == null) continue;
            int desc = n.getDescargasCount() != null ? n.getDescargasCount() : 0;
            descargasPorAnio.put(n.getAnio(), descargasPorAnio.getOrDefault(n.getAnio(), 0) + desc);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudios) {
            if (e.getAnio() == null) continue;
            int desc = e.getDescargasCount() != null ? e.getDescargasCount() : 0;
            descargasPorAnio.put(e.getAnio(), descargasPorAnio.getOrDefault(e.getAnio(), 0) + desc);
        }

        List<Map<String, ?>> tipoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tipoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tipoData.add(row);
        }
        List<Map<String, ?>> tematicaData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tematicaCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tematicaData.add(row);
        }
        List<Map<String, ?>> descargasData = new ArrayList<>();
        List<Integer> aniosOrdenados = new ArrayList<>(descargasPorAnio.keySet());
        aniosOrdenados.sort(Integer::compareTo);
        for (Integer anio : aniosOrdenados) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", anio.toString());
            row.put("value", descargasPorAnio.get(anio));
            descargasData.add(row);
        }

        JRMapCollectionDataSource chartData1 = new JRMapCollectionDataSource(tipoData);
        JRMapCollectionDataSource chartData2 = new JRMapCollectionDataSource(tematicaData);
        JRMapCollectionDataSource chartData3 = new JRMapCollectionDataSource(descargasData);

        byte[] pdf = reporteService().generarReportePDF("normativas_estudios_report", datos, null, chartData1, chartData2,
                chartData3);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_estudios_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/normativas.xlsx")
    protected void exportarNormativasXLSX(HttpServletResponse response,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        List<Map<String, Object>> datos = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (NormativaResponseDTO n : normativaService().listarNormativasUltimas100()) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo());
            row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre());
            row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    n.getCreatedAt() != null ? fmt.format(n.getCreatedAt().atZone(ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        for (org.reciclame.demo.dto.EstudioResponseDTO e : estudioService().listarEstudiosUltimos100()) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", e.getTitulo());
            row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", e.getEstadoActivo() != null && e.getEstadoActivo() ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion",
                    e.getCreatedAt() != null ? fmt.format(e.getCreatedAt().atZone(ZoneId.of("America/Lima")))
                            : "");
            datos.add(row);
        }

        Map<String, Integer> tipoCount = new LinkedHashMap<>();
        List<NormativaResponseDTO> normas = normativaService().listarNormativasUltimas100();
        List<org.reciclame.demo.dto.EstudioResponseDTO> estus = estudioService().listarEstudiosUltimos100();
        tipoCount.put("Normativa", normas.size());
        tipoCount.put("Estudio", estus.size());

        Map<String, Integer> tematicaCount = new LinkedHashMap<>();
        for (NormativaResponseDTO n : normas) {
            String key = n.getTematicaNombre() != null ? n.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estus) {
            String key = e.getTematicaNombre() != null ? e.getTematicaNombre() : "SIN_TEMATICA";
            tematicaCount.put(key, tematicaCount.getOrDefault(key, 0) + 1);
        }

        Map<Integer, Integer> descargasPorAnio = new LinkedHashMap<>();
        for (NormativaResponseDTO n : normas) {
            if (n.getAnio() == null) continue;
            int desc = n.getDescargasCount() != null ? n.getDescargasCount() : 0;
            descargasPorAnio.put(n.getAnio(), descargasPorAnio.getOrDefault(n.getAnio(), 0) + desc);
        }
        for (org.reciclame.demo.dto.EstudioResponseDTO e : estus) {
            if (e.getAnio() == null) continue;
            int desc = e.getDescargasCount() != null ? e.getDescargasCount() : 0;
            descargasPorAnio.put(e.getAnio(), descargasPorAnio.getOrDefault(e.getAnio(), 0) + desc);
        }

        List<Map<String, ?>> tipoData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tipoCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tipoData.add(row);
        }
        List<Map<String, ?>> tematicaData = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tematicaCount.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", entry.getKey());
            row.put("value", entry.getValue());
            tematicaData.add(row);
        }
        List<Map<String, ?>> descargasData = new ArrayList<>();
        List<Integer> aniosOrdenados = new ArrayList<>(descargasPorAnio.keySet());
        aniosOrdenados.sort(Integer::compareTo);
        for (Integer anio : aniosOrdenados) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", anio.toString());
            row.put("value", descargasPorAnio.get(anio));
            descargasData.add(row);
        }

        byte[] xlsx = reporteService().generarReporteExcelConGraficos("Reporte de Normativas y Estudios",
                "Últimos 100 documentos registrados",
                new String[] { "Titulo", "Tipo", "Tematica Nombre", "Estado", "Anio", "Descargas Count",
                        "Fecha Creacion" },
                datos, tipoData, tematicaData, descargasData);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"normativas_estudios_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    // ====================================================================
    //  CHATBOT ANALYTICS — exportes PDF / XLSX / CSV
    // ====================================================================

    protected Instant[] parseRango(LocalDate desde, LocalDate hasta) {
        ZoneId tz = ZoneId.of("America/Lima");
        Instant desdeI = desde != null ? desde.atStartOfDay(tz).toInstant() : null;
        Instant hastaI = hasta != null ? hasta.plusDays(1).atStartOfDay(tz).toInstant().minusMillis(1) : null;
        return new Instant[] { desdeI, hastaI };
    }

    @GetMapping("/admin/reportes/export/chatbot.pdf")
    protected void exportarChatbotPDF(HttpServletResponse response,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {
        Instant[] rango = parseRango(desde, hasta);
        var stats = chatbotService().calcularEstadisticas(rango[0], rango[1]);
        Page<ChatbotConsultaFilaDTO> page = chatbotService().listarConversacionesFiltrado(
                null, null, rango[0], rango[1], PageRequest.of(0, 500));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = new ArrayList<>();
        for (ChatbotConsultaFilaDTO f : page.getContent()) {
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", f.getFechaInicio() != null ? fmt.format(f.getFechaInicio()) : "");
            row.put("usuario", f.getUsuarioNombre());
            row.put("rol", f.getUsuarioRol());
            row.put("empresa", f.getUsuarioEmpresa() != null ? f.getUsuarioEmpresa() : "N/A");
            row.put("pregunta", f.getPrimeraPregunta());
            row.put("respuesta", f.getUltimaRespuesta());
            row.put("mensajes", f.getTotalMensajes());
            row.put("errores", f.getErrores());
            row.put("duracion", f.getDuracionTotalMs());
            row.put("idioma", f.getIdioma());
            datos.add(row);
        }

        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map<String, Object> r : stats.getConversacionesPorRol()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", r.get("rol"));
            row.put("value", ((Number) r.get("total")).intValue());
            rolData.add(row);
        }
        List<Map<String, ?>> mensajesData = new ArrayList<>();
        for (Map<String, Object> r : stats.getMensajesPorDia()) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", r.get("fecha"));
            row.put("value", ((Number) r.get("user")).intValue() + ((Number) r.get("bot")).intValue());
            mensajesData.add(row);
        }

        JRMapCollectionDataSource chart1 = new JRMapCollectionDataSource(rolData);
        JRMapCollectionDataSource chart2 = new JRMapCollectionDataSource(mensajesData);

        byte[] pdf = reporteService().generarReportePDF("chatbot_report", datos, null, chart1, chart2);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"chatbot_report.pdf\"");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/admin/reportes/export/chatbot.xlsx")
    protected void exportarChatbotXLSX(HttpServletResponse response,
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta)
            throws IOException {
        Instant[] rango = parseRango(desde, hasta);
        var stats = chatbotService().calcularEstadisticas(rango[0], rango[1]);
        Page<ChatbotConsultaFilaDTO> page = chatbotService().listarConversacionesFiltrado(
                null, null, rango[0], rango[1], PageRequest.of(0, 1000));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = new ArrayList<>();
        for (ChatbotConsultaFilaDTO f : page.getContent()) {
            Map<String, Object> row = new HashMap<>();
            row.put("fecha", f.getFechaInicio() != null ? fmt.format(f.getFechaInicio()) : "");
            row.put("usuario", f.getUsuarioNombre());
            row.put("rol", f.getUsuarioRol());
            row.put("empresa", f.getUsuarioEmpresa() != null ? f.getUsuarioEmpresa() : "N/A");
            row.put("pregunta", f.getPrimeraPregunta());
            row.put("respuesta", f.getUltimaRespuesta());
            row.put("mensajes", f.getTotalMensajes());
            row.put("errores", f.getErrores());
            row.put("duracion", f.getDuracionTotalMs());
            row.put("idioma", f.getIdioma());
            datos.add(row);
        }

        List<Map<String, ?>> rolData = new ArrayList<>();
        for (Map<String, Object> r : stats.getConversacionesPorRol()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", r.get("rol"));
            row.put("value", ((Number) r.get("total")).intValue());
            rolData.add(row);
        }
        List<Map<String, ?>> mensajesData = new ArrayList<>();
        for (Map<String, Object> r : stats.getMensajesPorDia()) {
            Map<String, Object> row = new HashMap<>();
            row.put("category", r.get("fecha"));
            row.put("value", ((Number) r.get("user")).intValue() + ((Number) r.get("bot")).intValue());
            mensajesData.add(row);
        }

        byte[] xlsx = reporteService().generarReporteExcelConGraficos(
                "Reporte de Chatbot Analytics",
                "Conversaciones registradas en el rango seleccionado",
                new String[] { "Fecha", "Usuario", "Rol", "Empresa", "Pregunta", "Respuesta",
                        "Mensajes", "Errores", "Duracion Ms", "Idioma" },
                datos, rolData, mensajesData);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"chatbot_report.xlsx\"");
        response.getOutputStream().write(xlsx);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Endpoints unificados para Auditoría, Normativas y Chatbot
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/admin/reportes/export/auditoria")
    @org.springframework.web.bind.annotation.ResponseBody
    protected ResponseEntity<?> exportarAuditoriaUnificado(
            @RequestBody ExportRequest req,
            @AuthenticationPrincipal UserDetails user) {

        String formato = req.formato != null ? req.formato.toLowerCase() : "pdf";
        int limite = req.limite != null ? req.limite : 100;
        String accion = req.accion != null ? req.accion.toLowerCase() : "download";

        List<Auditoria> auditorias = auditoriaService().listarAuditoriasLimit(limite);
        byte[] archivo;
        String nombreArchivo;
        String mime;

        if ("xlsx".equals(formato)) {
            archivo = construirAuditoriaXLSX(auditorias);
            nombreArchivo = "auditoria_report.xlsx";
            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            archivo = construirAuditoriaPDF(auditorias);
            nombreArchivo = "auditoria_report.pdf";
            mime = "application/pdf";
        }

        return entregar(archivo, nombreArchivo, mime, "Reporte de Auditoría",
                auditorias.size(), accion, user);
    }

    @PostMapping("/admin/reportes/export/normativas")
    @org.springframework.web.bind.annotation.ResponseBody
    protected ResponseEntity<?> exportarNormativasUnificado(
            @RequestBody ExportRequest req,
            @AuthenticationPrincipal UserDetails user) {

        String formato = req.formato != null ? req.formato.toLowerCase() : "pdf";
        int limite = req.limite != null ? req.limite : 100;
        String accion = req.accion != null ? req.accion.toLowerCase() : "download";

        List<NormativaResponseDTO> normativas = normativaService().listarNormativasLimit(limite);
        List<org.reciclame.demo.dto.EstudioResponseDTO> estudios = estudioService().listarEstudiosLimit(limite);
        byte[] archivo;
        String nombreArchivo;
        String mime;

        if ("xlsx".equals(formato)) {
            archivo = construirNormativasXLSX(normativas, estudios);
            nombreArchivo = "normativas_estudios_report.xlsx";
            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            archivo = construirNormativasPDF(normativas, estudios);
            nombreArchivo = "normativas_estudios_report.pdf";
            mime = "application/pdf";
        }

        return entregar(archivo, nombreArchivo, mime, "Reporte de Normativas y Estudios",
                normativas.size() + estudios.size(), accion, user);
    }

    @PostMapping("/admin/reportes/export/chatbot")
    @org.springframework.web.bind.annotation.ResponseBody
    protected ResponseEntity<?> exportarChatbotUnificado(
            @RequestBody ExportRequest req,
            @AuthenticationPrincipal UserDetails user) {

        String formato = req.formato != null ? req.formato.toLowerCase() : "pdf";
        int limite = req.limite != null ? req.limite : 100;
        String accion = req.accion != null ? req.accion.toLowerCase() : "download";

        int pageSize = limite > 0 ? limite : 5000;
        var stats = chatbotService().calcularEstadisticas(null, null);
        Page<ChatbotConsultaFilaDTO> page = chatbotService().listarConversacionesFiltrado(
                null, null, null, null, PageRequest.of(0, pageSize));

        byte[] archivo;
        String nombreArchivo;
        String mime;

        if ("xlsx".equals(formato)) {
            archivo = construirChatbotXLSX(page, stats);
            nombreArchivo = "chatbot_report.xlsx";
            mime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            archivo = construirChatbotPDF(page, stats);
            nombreArchivo = "chatbot_report.pdf";
            mime = "application/pdf";
        }

        return entregar(archivo, nombreArchivo, mime, "Reporte de Chatbot",
                page.getNumberOfElements(), accion, user);
    }

    private ResponseEntity<?> entregar(byte[] archivo, String nombreArchivo, String mime,
                                       String tituloReporte, int totalRegistros,
                                       String accion, UserDetails user) {
        if ("email".equals(accion) || "both".equals(accion)) {
            String correo = user != null ? user.getUsername() : null;
            String nombre = correo;
            try {
                var dto = usuarioService().findByCorreo(correo).orElse(null);
                if (dto != null && dto.getNombreCompleto() != null) nombre = dto.getNombreCompleto();
            } catch (Exception ignored) {}
            emailNotificationService().enviarReporteAdjunto(
                    correo, nombre, tituloReporte, totalRegistros, archivo, nombreArchivo, mime);
            if ("email".equals(accion)) {
                ExportResult r = new ExportResult();
                r.enviado = true; r.descarga = false;
                r.totalRegistros = totalRegistros;
                r.correo = correo;
                r.mensaje = "Reporte enviado a " + correo;
                return ResponseEntity.ok(r);
            }
        }
        return ResponseEntity.ok()
                .header("Content-Type", mime)
                .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                .header("X-Registros", String.valueOf(totalRegistros))
                .header("X-Email-Sent", String.valueOf("both".equals(accion)))
                .body(archivo);
    }

    // ── HELPERS auditoría ─────────────────────────────────────────────────
    private byte[] construirAuditoriaPDF(List<Auditoria> auditorias) {
        List<Map<String, Object>> datos = auditorias.stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("fechaAccion", a.getFechaAccion());
            row.put("accion", a.getAccion());
            row.put("modulo", a.getModulo());
            row.put("estado", a.getEstado());
            row.put("usuarioNombre", a.getUsuarioNombre());
            row.put("ipAddress", a.getIpAddress());
            row.put("descripcion", a.getDescripcion());
            return row;
        }).toList();
        Map<String, Integer> accionesCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (Auditoria a : auditorias) {
            accionesCount.merge(a.getAccion() != null ? a.getAccion() : "SIN_ACCION", 1, Integer::sum);
            estadoCount.merge(a.getEstado() != null ? a.getEstado() : "SIN_ESTADO", 1, Integer::sum);
        }
        List<Map<String, ?>> accionesData = new ArrayList<>();
        for (var e : accionesCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); accionesData.add(m); }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (var e : estadoCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); estadoData.add(m); }
        return reporteService().generarReportePDF("auditoria_report", datos, null,
                new JRMapCollectionDataSource(accionesData),
                new JRMapCollectionDataSource(estadoData));
    }

    private byte[] construirAuditoriaXLSX(List<Auditoria> auditorias) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
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
        }).toList();
        Map<String, Integer> accionesCount = new LinkedHashMap<>();
        Map<String, Integer> estadoCount = new LinkedHashMap<>();
        for (Auditoria a : auditorias) {
            accionesCount.merge(a.getAccion() != null ? a.getAccion() : "SIN_ACCION", 1, Integer::sum);
            estadoCount.merge(a.getEstado() != null ? a.getEstado() : "SIN_ESTADO", 1, Integer::sum);
        }
        List<Map<String, ?>> accionesData = new ArrayList<>();
        for (var e : accionesCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); accionesData.add(m); }
        List<Map<String, ?>> estadoData = new ArrayList<>();
        for (var e : estadoCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); estadoData.add(m); }
        return reporteService().generarReporteExcelConGraficos("Reporte de Auditoría",
                "Total: " + auditorias.size() + " eventos",
                new String[]{"Fecha","Acción","Módulo","Estado","Usuario","IP","Descripción"},
                datos, accionesData, estadoData);
    }

    // ── HELPERS normativas + estudios ─────────────────────────────────────
    private byte[] construirNormativasPDF(List<NormativaResponseDTO> normativas,
                                           List<org.reciclame.demo.dto.EstudioResponseDTO> estudios) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> datos = new ArrayList<>();
        for (NormativaResponseDTO n : normativas) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo()); row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre()); row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion", n.getCreatedAt()!=null?fmt.format(n.getCreatedAt().atZone(ZoneId.of("America/Lima"))):"");
            datos.add(row);
        }
        for (var e : estudios) {
            Map<String, Object> row = new HashMap<>();
            row.put("titulo", e.getTitulo()); row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", Boolean.TRUE.equals(e.getEstadoActivo()) ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion", e.getCreatedAt()!=null?fmt.format(e.getCreatedAt().atZone(ZoneId.of("America/Lima"))):"");
            datos.add(row);
        }
        Map<String, Integer> tipoCount = new LinkedHashMap<>();
        tipoCount.put("Normativa", normativas.size());
        tipoCount.put("Estudio", estudios.size());
        Map<String, Integer> tematicaCount = new LinkedHashMap<>();
        for (var n : normativas) tematicaCount.merge(n.getTematicaNombre()!=null?n.getTematicaNombre():"SIN_TEMATICA", 1, Integer::sum);
        for (var e : estudios) tematicaCount.merge(e.getTematicaNombre()!=null?e.getTematicaNombre():"SIN_TEMATICA", 1, Integer::sum);
        Map<Integer, Integer> descPorAnio = new LinkedHashMap<>();
        for (var n : normativas) if (n.getAnio()!=null) descPorAnio.merge(n.getAnio(), n.getDescargasCount()!=null?n.getDescargasCount():0, Integer::sum);
        for (var e : estudios) if (e.getAnio()!=null) descPorAnio.merge(e.getAnio(), e.getDescargasCount()!=null?e.getDescargasCount():0, Integer::sum);

        List<Map<String, ?>> tipoData = new ArrayList<>();
        for (var x : tipoCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",x.getKey()); m.put("value",x.getValue()); tipoData.add(m); }
        List<Map<String, ?>> tematicaData = new ArrayList<>();
        for (var x : tematicaCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",x.getKey()); m.put("value",x.getValue()); tematicaData.add(m); }
        List<Map<String, ?>> descargasData = new ArrayList<>();
        List<Integer> anios = new ArrayList<>(descPorAnio.keySet()); anios.sort(Integer::compareTo);
        for (Integer a : anios) { Map<String,Object> m=new HashMap<>(); m.put("category", a.toString()); m.put("value", descPorAnio.get(a)); descargasData.add(m); }

        return reporteService().generarReportePDF("normativas_estudios_report", datos, null,
                new JRMapCollectionDataSource(tipoData),
                new JRMapCollectionDataSource(tematicaData),
                new JRMapCollectionDataSource(descargasData));
    }

    private byte[] construirNormativasXLSX(List<NormativaResponseDTO> normativas,
                                            List<org.reciclame.demo.dto.EstudioResponseDTO> estudios) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> datos = new ArrayList<>();
        for (var n : normativas) {
            Map<String,Object> row = new HashMap<>();
            row.put("titulo", n.getTitulo()); row.put("tipo", "Normativa");
            row.put("tematicaNombre", n.getTematicaNombre()); row.put("estado", n.getEstadoNormativaNombre());
            row.put("anio", n.getAnio());
            row.put("descargasCount", n.getDescargasCount() != null ? n.getDescargasCount() : 0);
            row.put("fechaCreacion", n.getCreatedAt()!=null?fmt.format(n.getCreatedAt().atZone(ZoneId.of("America/Lima"))):"");
            datos.add(row);
        }
        for (var e : estudios) {
            Map<String,Object> row = new HashMap<>();
            row.put("titulo", e.getTitulo()); row.put("tipo", "Estudio");
            row.put("tematicaNombre", e.getTematicaNombre());
            row.put("estado", Boolean.TRUE.equals(e.getEstadoActivo()) ? "Activo" : "Inactivo");
            row.put("anio", e.getAnio());
            row.put("descargasCount", e.getDescargasCount() != null ? e.getDescargasCount() : 0);
            row.put("fechaCreacion", e.getCreatedAt()!=null?fmt.format(e.getCreatedAt().atZone(ZoneId.of("America/Lima"))):"");
            datos.add(row);
        }
        Map<String, Integer> tipoCount = new LinkedHashMap<>();
        tipoCount.put("Normativa", normativas.size());
        tipoCount.put("Estudio", estudios.size());
        Map<String, Integer> tematicaCount = new LinkedHashMap<>();
        for (var n : normativas) tematicaCount.merge(n.getTematicaNombre()!=null?n.getTematicaNombre():"SIN_TEMATICA", 1, Integer::sum);
        for (var e : estudios) tematicaCount.merge(e.getTematicaNombre()!=null?e.getTematicaNombre():"SIN_TEMATICA", 1, Integer::sum);
        Map<Integer, Integer> descPorAnio = new LinkedHashMap<>();
        for (var n : normativas) if (n.getAnio()!=null) descPorAnio.merge(n.getAnio(), n.getDescargasCount()!=null?n.getDescargasCount():0, Integer::sum);
        for (var e : estudios) if (e.getAnio()!=null) descPorAnio.merge(e.getAnio(), e.getDescargasCount()!=null?e.getDescargasCount():0, Integer::sum);
        List<Map<String, ?>> tipoData = new ArrayList<>();
        for (var x : tipoCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",x.getKey()); m.put("value",x.getValue()); tipoData.add(m); }
        List<Map<String, ?>> tematicaData = new ArrayList<>();
        for (var x : tematicaCount.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",x.getKey()); m.put("value",x.getValue()); tematicaData.add(m); }
        List<Map<String, ?>> descargasData = new ArrayList<>();
        List<Integer> anios = new ArrayList<>(descPorAnio.keySet()); anios.sort(Integer::compareTo);
        for (Integer a : anios) { Map<String,Object> m=new HashMap<>(); m.put("category", a.toString()); m.put("value", descPorAnio.get(a)); descargasData.add(m); }
        return reporteService().generarReporteExcelConGraficos("Reporte de Normativas y Estudios",
                "Total: " + (normativas.size() + estudios.size()) + " documentos",
                new String[]{"Titulo","Tipo","Tematica","Estado","Año","Descargas","Fecha Creacion"},
                datos, tipoData, tematicaData, descargasData);
    }

    // ── HELPERS chatbot ───────────────────────────────────────────────────
    private byte[] construirChatbotPDF(Page<ChatbotConsultaFilaDTO> page, Object stats) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = new ArrayList<>();
        Map<String, Integer> rolMap = new LinkedHashMap<>();
        Map<String, Integer> mensajesPorDia = new LinkedHashMap<>();
        DateTimeFormatter dia = DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.of("America/Lima"));
        for (var f : page.getContent()) {
            Map<String,Object> row = new HashMap<>();
            row.put("fecha", f.getFechaInicio() != null ? fmt.format(f.getFechaInicio()) : "");
            row.put("usuario", f.getUsuarioNombre());
            row.put("rol", f.getUsuarioRol());
            row.put("empresa", f.getUsuarioEmpresa() != null ? f.getUsuarioEmpresa() : "N/A");
            row.put("pregunta", f.getPrimeraPregunta());
            row.put("respuesta", f.getUltimaRespuesta());
            row.put("mensajes", f.getTotalMensajes());
            row.put("errores", f.getErrores());
            row.put("duracion", f.getDuracionTotalMs());
            row.put("idioma", f.getIdioma());
            datos.add(row);
            rolMap.merge(f.getUsuarioRol() != null ? f.getUsuarioRol() : "SIN_ROL", 1, Integer::sum);
            if (f.getFechaInicio() != null) mensajesPorDia.merge(dia.format(f.getFechaInicio()),
                    f.getTotalMensajes()!=null?f.getTotalMensajes():0, Integer::sum);
        }
        List<Map<String, ?>> rolData = new ArrayList<>();
        for (var e : rolMap.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); rolData.add(m); }
        List<Map<String, ?>> mensajesData = new ArrayList<>();
        for (var e : mensajesPorDia.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("category",e.getKey()); m.put("value",e.getValue()); mensajesData.add(m); }
        return reporteService().generarReportePDF("chatbot_report", datos, null,
                new JRMapCollectionDataSource(rolData),
                new JRMapCollectionDataSource(mensajesData));
    }

    private byte[] construirChatbotXLSX(Page<ChatbotConsultaFilaDTO> page, Object stats) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Lima"));
        List<Map<String, Object>> datos = new ArrayList<>();
        Map<String, Integer> rolMap = new LinkedHashMap<>();
        Map<String, Integer> mensajesPorDia = new LinkedHashMap<>();
        DateTimeFormatter dia = DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.of("America/Lima"));
        for (var f : page.getContent()) {
            Map<String,Object> row = new HashMap<>();
            row.put("fecha", f.getFechaInicio() != null ? fmt.format(f.getFechaInicio()) : "");
            row.put("usuario", f.getUsuarioNombre());
            row.put("rol", f.getUsuarioRol());
            row.put("empresa", f.getUsuarioEmpresa() != null ? f.getUsuarioEmpresa() : "N/A");
            row.put("pregunta", f.getPrimeraPregunta());
            row.put("respuesta", f.getUltimaRespuesta());
            row.put("mensajes", f.getTotalMensajes());
            row.put("errores", f.getErrores());
            row.put("duracion", f.getDuracionTotalMs());
            row.put("idioma", f.getIdioma());
            datos.add(row);
            rolMap.merge(f.getUsuarioRol() != null ? f.getUsuarioRol() : "SIN_ROL", 1, Integer::sum);
            if (f.getFechaInicio() != null) mensajesPorDia.merge(dia.format(f.getFechaInicio()),
                    f.getTotalMensajes()!=null?f.getTotalMensajes():0, Integer::sum);
        }
        List<Map<String, ?>> rolData = new ArrayList<>();
        for (var e : rolMap.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("label",e.getKey()); m.put("value",e.getValue()); rolData.add(m); }
        List<Map<String, ?>> mensajesData = new ArrayList<>();
        for (var e : mensajesPorDia.entrySet()) { Map<String,Object> m=new HashMap<>(); m.put("category",e.getKey()); m.put("value",e.getValue()); mensajesData.add(m); }
        return reporteService().generarReporteExcelConGraficos("Reporte de Chatbot",
                "Total: " + page.getNumberOfElements() + " conversaciones",
                new String[]{"Fecha","Usuario","Rol","Empresa","Pregunta","Respuesta","Mensajes","Errores","Duración Ms","Idioma"},
                datos, rolData, mensajesData);
    }
}
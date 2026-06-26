package org.reciclame.demo.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    // ── Brand palette ──────────────────────────────────────────────────────
    private static final byte[] NAVY      = {(byte)0x1B,(byte)0x3A,(byte)0x5C};
    private static final byte[] GRN_DARK  = {(byte)0x27,(byte)0xAE,(byte)0x60};
    private static final byte[] ROW_ALT   = {(byte)0xF0,(byte)0xFF,(byte)0xF4};
    private static final byte[] GRAY_TXT  = {(byte)0x6B,(byte)0x72,(byte)0x80};
    private static final byte[] BORDER_CL = {(byte)0xD1,(byte)0xD5,(byte)0xDB};
    private static final byte[] WHITE     = {(byte)0xFF,(byte)0xFF,(byte)0xFF};
    private static final byte[] NAVY_FOOT = {(byte)0x0F,(byte)0x25,(byte)0x40};

    // ── Jasper helpers ─────────────────────────────────────────────────────

    private JasperReport cargarReporte(String reporte) throws JRException {
        InputStream jasperStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jasper");
        if (jasperStream != null) return (JasperReport) JRLoader.loadObject(jasperStream);
        InputStream jrxmlStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jrxml");
        if (jrxmlStream == null) throw new RuntimeException("No se encontró el reporte: " + reporte);
        return JasperCompileManager.compileReport(jrxmlStream);
    }

    public byte[] generarReport(String reporte) {
        try {
            JasperReport jr = cargarReporte(reporte);
            JasperPrint jp = JasperFillManager.fillReport(jr, new java.util.HashMap<>(), new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jp);
        } catch (JRException e) { throw new RuntimeException("Error generating report: " + reporte, e); }
    }

    public byte[] generarReportePDF(String reporte, JRDataSource dataSource, Map<String, Object> params) {
        try {
            JasperReport jr = cargarReporte(reporte);
            JasperPrint jp = JasperFillManager.fillReport(jr, params != null ? params : new java.util.HashMap<>(), dataSource);
            return JasperExportManager.exportReportToPdf(jp);
        } catch (JRException e) { throw new RuntimeException("Error generating PDF report: " + reporte, e); }
    }

    public byte[] generarReportePDF(String reporte, List<?> data, Map<String, Object> params) {
        return generarReportePDF(reporte, new JRBeanCollectionDataSource(data), params);
    }

    public byte[] generarReportePDF(String reporte, List<?> mainData, Map<String, Object> params,
                                    JRDataSource... chartDataSources) {
        try {
            JasperReport jr = cargarReporte(reporte);
            Map<String, Object> fp = params != null ? params : new java.util.HashMap<>();
            for (int i = 0; i < chartDataSources.length; i++) fp.put("chartData" + (i + 1), chartDataSources[i]);
            JasperPrint jp = JasperFillManager.fillReport(jr, fp, new JRBeanCollectionDataSource(mainData));
            return JasperExportManager.exportReportToPdf(jp);
        } catch (JRException e) { throw new RuntimeException("Error generating PDF report: " + reporte, e); }
    }

    // ── Excel — tabla de datos limpia ──────────────────────────────────────

    public byte[] generarReporteExcel(String title, String subtitle, String[] headers,
                                       List<Map<String, Object>> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet(title.length() > 31 ? title.substring(0, 31) : title);
            buildDataSheet(wb, sheet, title, subtitle, headers, data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generating Excel: " + title, e); }
    }

    // ── Excel — tabla + hoja de gráficos nativos ───────────────────────────

    public byte[] generarReporteExcelConGraficos(String title, String subtitle, String[] headers,
                                                  List<Map<String, Object>> data,
                                                  List<Map<String, ?>> chart1Data,
                                                  List<Map<String, ?>> chart2Data) {
        return generarReporteExcelConGraficos(title, subtitle, headers, data, chart1Data, chart2Data, null);
    }

    public byte[] generarReporteExcelConGraficos(String title, String subtitle, String[] headers,
                                                  List<Map<String, Object>> data,
                                                  List<Map<String, ?>> chart1Data,
                                                  List<Map<String, ?>> chart2Data,
                                                  List<Map<String, ?>> chart3Data) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Hoja 1: datos
            String safeTitle = title.length() > 31 ? title.substring(0, 31) : title;
            XSSFSheet dataSheet = wb.createSheet(safeTitle);
            buildDataSheet(wb, dataSheet, title, subtitle, headers, data);

            // Hoja 2: gráficos nativos
            XSSFSheet chartSheet = wb.createSheet("Gráficos");
            buildChartSheet(wb, chartSheet, title, chart1Data, chart2Data, chart3Data);

            // Activar hoja de datos al abrir
            wb.setActiveSheet(0);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel with charts: " + title, e);
        }
    }

    // ── Construcción de hoja de datos ─────────────────────────────────────

    private void buildDataSheet(XSSFWorkbook wb, XSSFSheet sheet, String title, String subtitle,
                                 String[] headers, List<Map<String, Object>> data) {
        sheet.setDefaultColumnWidth(18);
        int ri = 0;

        // Franja de marca
        XSSFRow brandRow = sheet.createRow(ri++);
        brandRow.setHeight((short) 900);
        XSSFCell brandCell = brandRow.createCell(0);
        brandCell.setCellValue("♻ RECICLAME  —  Repositorio Circular de Economía Circular");
        brandCell.setCellStyle(buildBrandStyle(wb));
        mergeRow(sheet, ri - 1, headers.length);

        // Título
        XSSFRow titleRow = sheet.createRow(ri++);
        titleRow.setHeight((short) 750);
        XSSFCell tc = titleRow.createCell(0);
        tc.setCellValue(title);
        tc.setCellStyle(buildTitleStyle(wb));
        mergeRow(sheet, ri - 1, headers.length);

        // Subtítulo
        XSSFRow subRow = sheet.createRow(ri++);
        subRow.setHeight((short) 420);
        XSSFCell sc = subRow.createCell(0);
        sc.setCellValue(subtitle);
        sc.setCellStyle(buildSubtitleStyle(wb));
        mergeRow(sheet, ri - 1, headers.length);

        // Separador
        sheet.createRow(ri++).setHeight((short) 180);

        // Encabezados de columna
        XSSFRow hdrRow = sheet.createRow(ri++);
        hdrRow.setHeight((short) 480);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell c = hdrRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(buildHeaderStyle(wb));
        }

        // Filas de datos
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            XSSFRow dr = sheet.createRow(ri++);
            dr.setHeight((short) 360);
            XSSFCellStyle cs = buildDataStyle(wb, i % 2 == 1);
            for (int j = 0; j < headers.length; j++) {
                XSSFCell c = dr.createCell(j);
                Object v = row.get(toCamelCase(headers[j]));
                c.setCellValue(v != null ? v.toString() : "");
                c.setCellStyle(cs);
            }
        }

        // Separador
        sheet.createRow(ri++).setHeight((short) 180);

        // Pie de página
        XSSFRow footRow = sheet.createRow(ri);
        footRow.setHeight((short) 420);
        XSSFCell fc = footRow.createCell(0);
        fc.setCellValue("Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + "   |   reciclame.org");
        fc.setCellStyle(buildFooterStyle(wb));
        mergeRow(sheet, ri, headers.length);

        autoSizeCols(sheet, headers.length);
    }

    // ── Construcción de hoja de gráficos ──────────────────────────────────

    private void buildChartSheet(XSSFWorkbook wb, XSSFSheet sheet, String reportTitle,
                                  List<Map<String, ?>> chart1Data,
                                  List<Map<String, ?>> chart2Data,
                                  List<Map<String, ?>> chart3Data) {
        sheet.setDefaultColumnWidth(14);

        // Título de la hoja
        XSSFRow titleRow = sheet.createRow(0);
        titleRow.setHeight((short) 700);
        XSSFCell tc = titleRow.createCell(0);
        tc.setCellValue("Gráficos — " + reportTitle);
        tc.setCellStyle(buildBrandStyle(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 18));

        int chartRow = 2;

        if (chart1Data != null && !chart1Data.isEmpty()) {
            addChartSectionLabel(wb, sheet, chartRow, 0, "Distribución por Categoría");
            addNativePieChart(sheet, chart1Data, chartRow + 1, 0, chartRow + 18, 9);
            chartRow += 20;
        }
        if (chart2Data != null && !chart2Data.isEmpty()) {
            addChartSectionLabel(wb, sheet, chartRow, 0, "Distribución Secundaria");
            addNativePieChart(sheet, chart2Data, chartRow + 1, 0, chartRow + 18, 9);
            chartRow += 20;
        }
        if (chart3Data != null && !chart3Data.isEmpty()) {
            addChartSectionLabel(wb, sheet, chartRow, 0, "Evolución por Período");
            addNativeBarChart(sheet, chart3Data, chartRow + 1, 0, chartRow + 18, 9);
        }
    }

    private void addChartSectionLabel(XSSFWorkbook wb, XSSFSheet sheet, int row, int col, String label) {
        XSSFRow r = sheet.createRow(row);
        r.setHeight((short) 420);
        XSSFCell c = r.createCell(col);
        c.setCellValue(label);
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setBold(true);
        f.setFontHeightInPoints((short) 12);
        f.setColor(new XSSFColor(NAVY, null));
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(ROW_ALT, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setBorderBottom(BorderStyle.MEDIUM);
        st.setBottomBorderColor(new XSSFColor(GRN_DARK, null));
        c.setCellStyle(st);
        sheet.addMergedRegion(new CellRangeAddress(row, row, col, col + 8));
    }

    // ── Gráfico de torta nativo (XDDF) ────────────────────────────────────

    private void addNativePieChart(XSSFSheet sheet, List<Map<String, ?>> data,
                                    int r1, int c1, int r2, int c2) {
        if (data == null || data.isEmpty()) return;
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, c1, r1, c2, r2);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleOverlay(false);

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            String[] labels = data.stream()
                    .map(m -> m.get("label") != null ? m.get("label").toString() : "")
                    .toArray(String[]::new);
            Double[] values = data.stream()
                    .map(m -> { Object v = m.get("value"); return v instanceof Number ? ((Number)v).doubleValue() : 0.0; })
                    .toArray(Double[]::new);

            XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromArray(labels);
            XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromArray(values);

            XDDFPieChartData pieData = (XDDFPieChartData) chart.createData(ChartTypes.PIE, null, null);
            pieData.setVaryColors(true);
            XDDFPieChartData.Series series = (XDDFPieChartData.Series) pieData.addSeries(cats, vals);
            series.setExplosion(0L);
            chart.plot(pieData);
        } catch (Exception e) {
            throw new RuntimeException("Error creating native pie chart", e);
        }
    }

    // ── Gráfico de barras nativo (XDDF) ───────────────────────────────────

    private void addNativeBarChart(XSSFSheet sheet, List<Map<String, ?>> data,
                                    int r1, int c1, int r2, int c2) {
        if (data == null || data.isEmpty()) return;
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, c1, r1, c2, r2);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleOverlay(false);

            XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            xAxis.setVisible(true);
            XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);
            yAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            String[] cats = data.stream()
                    .map(m -> {
                        if (m.get("category") != null) return m.get("category").toString();
                        if (m.get("label") != null) return m.get("label").toString();
                        return "";
                    })
                    .toArray(String[]::new);
            Double[] vals = data.stream()
                    .map(m -> { Object v = m.get("value"); return v instanceof Number ? ((Number)v).doubleValue() : 0.0; })
                    .toArray(Double[]::new);

            XDDFBarChartData barData = (XDDFBarChartData) chart.createData(ChartTypes.BAR, xAxis, yAxis);
            barData.setBarDirection(BarDirection.COL);
            barData.setVaryColors(true);

            XDDFBarChartData.Series series = (XDDFBarChartData.Series) barData.addSeries(
                    XDDFDataSourcesFactory.fromArray(cats),
                    XDDFDataSourcesFactory.fromArray(vals)
            );
            series.setTitle("Valor", null);
            series.setShowLeaderLines(false);
            chart.plot(barData);
        } catch (Exception e) {
            throw new RuntimeException("Error creating native bar chart", e);
        }
    }

    // ── Estilos de celda ──────────────────────────────────────────────────

    private XSSFCellStyle buildBrandStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 14);
        f.setBold(true);
        f.setColor(new XSSFColor(WHITE, null));
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(NAVY, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }

    private XSSFCellStyle buildTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 22);
        f.setBold(true);
        f.setColor(new XSSFColor(NAVY, null));
        st.setFont(f);
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setBorderLeft(BorderStyle.THICK);
        st.setLeftBorderColor(new XSSFColor(GRN_DARK, null));
        return st;
    }

    private XSSFCellStyle buildSubtitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(GRAY_TXT, null));
        st.setFont(f);
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setBorderLeft(BorderStyle.THICK);
        st.setLeftBorderColor(new XSSFColor(GRN_DARK, null));
        return st;
    }

    private XSSFCellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 11);
        f.setBold(true);
        f.setColor(new XSSFColor(WHITE, null));
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(GRN_DARK, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        st.setBorderBottom(BorderStyle.MEDIUM);
        st.setBottomBorderColor(new XSSFColor(NAVY, null));
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setTopBorderColor(new XSSFColor(BORDER_CL, null));
        st.setLeftBorderColor(new XSSFColor(BORDER_CL, null));
        st.setRightBorderColor(new XSSFColor(BORDER_CL, null));
        st.setWrapText(true);
        return st;
    }

    private XSSFCellStyle buildDataStyle(XSSFWorkbook wb, boolean alt) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(new byte[]{(byte)0x1F,(byte)0x29,(byte)0x37}, null));
        st.setFont(f);
        if (alt) {
            st.setFillForegroundColor(new XSSFColor(ROW_ALT, null));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        st.setBorderBottom(BorderStyle.THIN);
        st.setBorderTop(BorderStyle.THIN);
        st.setBorderLeft(BorderStyle.THIN);
        st.setBorderRight(BorderStyle.THIN);
        st.setBottomBorderColor(new XSSFColor(BORDER_CL, null));
        st.setTopBorderColor(new XSSFColor(BORDER_CL, null));
        st.setLeftBorderColor(new XSSFColor(BORDER_CL, null));
        st.setRightBorderColor(new XSSFColor(BORDER_CL, null));
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }

    private XSSFCellStyle buildFooterStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontName("Calibri");
        f.setFontHeightInPoints((short) 9);
        f.setColor(new XSSFColor(WHITE, null));
        st.setFont(f);
        st.setFillForegroundColor(new XSSFColor(NAVY_FOOT, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        return st;
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    private void mergeRow(XSSFSheet sheet, int row, int cols) {
        if (cols > 1) sheet.addMergedRegion(new CellRangeAddress(row, row, 0, cols - 1));
    }

    private void autoSizeCols(XSSFSheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            try { sheet.autoSizeColumn(i); } catch (Exception ignored) {}
            if (sheet.getColumnWidth(i) > 20000) sheet.setColumnWidth(i, 20000);
        }
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        String norm = str.toLowerCase()
            .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u")
            .replace("ñ","n").replace("ü","u");
        String[] words = norm.split("\\s+");
        StringBuilder sb = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty())
                sb.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return sb.toString();
    }
}

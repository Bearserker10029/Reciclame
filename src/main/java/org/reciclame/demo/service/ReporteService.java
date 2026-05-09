package org.reciclame.demo.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final short HEADER_BG = 0x006699;

    private JasperReport cargarReporte(String reporte) throws JRException {
        InputStream jasperStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jasper");
        if (jasperStream != null) {
            return (JasperReport) JRLoader.loadObject(jasperStream);
        }
        InputStream jrxmlStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jrxml");
        if (jrxmlStream == null) {
            throw new RuntimeException("No se encontró el reporte: " + reporte);
        }
        return JasperCompileManager.compileReport(jrxmlStream);
    }

    public byte[] generarReport(String reporte) {
        try {
            JasperReport jasperReport = cargarReporte(reporte);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, new java.util.HashMap<>(), new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Error generating report: " + reporte, e);
        }
    }

    public byte[] generarReportePDF(String reporte, JRDataSource dataSource, Map<String, Object> params) {
        try {
            JasperReport jasperReport = cargarReporte(reporte);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params != null ? params : new java.util.HashMap<>(), dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Error generating PDF report: " + reporte, e);
        }
    }

    public byte[] generarReportePDF(String reporte, List<?> data, Map<String, Object> params) {
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
        return generarReportePDF(reporte, dataSource, params);
    }

    public byte[] generarReportePDF(String reporte, List<?> mainData, Map<String, Object> params,
                                    JRDataSource... chartDataSources) {
        try {
            JasperReport jasperReport = cargarReporte(reporte);

            Map<String, Object> finalParams = params != null ? params : new java.util.HashMap<>();
            for (int i = 0; i < chartDataSources.length; i++) {
                finalParams.put("chartData" + (i + 1), chartDataSources[i]);
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(mainData);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, finalParams, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Error generating PDF report: " + reporte, e);
        }
    }

    public byte[] generarReporteExcel(String title, String subtitle, String[] headers, List<Map<String, Object>> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(title);

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            int rowIndex = 0;

            XSSFRow titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeight((short) 900);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            XSSFCellStyle titleStyle = createTitleStyle(workbook);
            titleCell.setCellStyle(titleStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
            }

            XSSFRow subtitleRow = sheet.createRow(rowIndex++);
            subtitleRow.setHeight((short) 400);
            XSSFCell subCell = subtitleRow.createCell(0);
            subCell.setCellValue(subtitle);
            XSSFCellStyle subStyle = createSubtitleStyle(workbook);
            subCell.setCellStyle(subStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));
            }

            rowIndex++;

            XSSFRow headerRow = sheet.createRow(rowIndex++);
            headerRow.setHeight((short) 400);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> row = data.get(i);
                XSSFRow dataRow = sheet.createRow(rowIndex++);
                dataRow.setHeight((short) 350);

                int colIndex = 0;
                for (String header : headers) {
                    XSSFCell cell = dataRow.createCell(colIndex++);
                    Object value = row.get(toCamelCase(header));
                    if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(createDataStyle(workbook, i % 2 == 0));
                }
            }

            int footerRow = rowIndex + 2;
            XSSFRow footRow = sheet.createRow(footerRow);
            footRow.setHeight((short) 350);
            XSSFCell dateCell = footRow.createCell(0);
            dateCell.setCellValue("Fecha de generación: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            XSSFCellStyle footerStyle = createFooterStyle(workbook);
            dateCell.setCellStyle(footerStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(footerRow, footerRow, 0, headers.length - 1));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel report: " + title, e);
        }
    }

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
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(title);

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            int rowIndex = 0;

            XSSFRow titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeight((short) 900);
            XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            XSSFCellStyle titleStyle = createTitleStyle(workbook);
            titleCell.setCellStyle(titleStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
            }

            XSSFRow subtitleRow = sheet.createRow(rowIndex++);
            subtitleRow.setHeight((short) 400);
            XSSFCell subCell = subtitleRow.createCell(0);
            subCell.setCellValue(subtitle);
            XSSFCellStyle subStyle = createSubtitleStyle(workbook);
            subCell.setCellStyle(subStyle);
            if (headers.length > 1) {
                sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, headers.length - 1));
            }

            rowIndex++;

            XSSFRow headerRow = sheet.createRow(rowIndex++);
            headerRow.setHeight((short) 400);
            for (int i = 0; i < headers.length; i++) {
                XSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> row = data.get(i);
                XSSFRow dataRow = sheet.createRow(rowIndex++);
                dataRow.setHeight((short) 350);

                int colIndex = 0;
                for (String header : headers) {
                    XSSFCell cell = dataRow.createCell(colIndex++);
                    Object value = row.get(toCamelCase(header));
                    if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(createDataStyle(workbook, i % 2 == 0));
                }
            }

            int chartsStartRow = rowIndex + 2;
            int chartColOffset = headers.length + 2;

            if (chart1Data != null && !chart1Data.isEmpty()) {
                createPieChart(workbook, sheet, "Chart 1: Distribution", chart1Data, chartsStartRow, chartColOffset);
            }
            if (chart2Data != null && !chart2Data.isEmpty()) {
                createPieChart(workbook, sheet, "Chart 2: Distribution", chart2Data, chartsStartRow + 18, chartColOffset);
            }
            if (chart3Data != null && !chart3Data.isEmpty()) {
                createBarChart(workbook, sheet, "Chart 3: By Category", chart3Data, chartsStartRow + 36, chartColOffset);
            }

            int footerRow = rowIndex + 2 + (chart3Data != null ? 54 : (chart2Data != null ? 36 : 18));
            if (footerRow < sheet.getLastRowNum() + 4) {
                XSSFRow footRow = sheet.createRow(footerRow);
                footRow.setHeight((short) 350);
                XSSFCell dateCell = footRow.createCell(0);
                dateCell.setCellValue("Fecha de generación: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                XSSFCellStyle footerStyle = createFooterStyle(workbook);
                dateCell.setCellStyle(footerStyle);
                if (headers.length > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(footerRow, footerRow, 0, headers.length - 1));
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating Excel report with charts: " + title, e);
        }
    }

    private void createPieChart(XSSFWorkbook workbook, XSSFSheet sheet, String title, List<Map<String, ?>> data, int startRow, int startCol) {
        if (data == null || data.isEmpty()) return;

        try {
            // Crear dataset para el gráfico pie
            DefaultPieDataset dataset = new DefaultPieDataset();
            for (Map<String, ?> row : data) {
                String label = row.get("label") != null ? row.get("label").toString() : "";
                Object val = row.get("value");
                double value = 0;
                if (val instanceof Number) {
                    value = ((Number) val).doubleValue();
                } else if (val != null) {
                    try {
                        value = Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        value = 0;
                    }
                }
                dataset.setValue(label, value);
            }

            // Crear gráfico pie
            JFreeChart pieChart = ChartFactory.createPieChart(
                title,
                dataset,
                true,  // legend
                false, // tooltips
                false  // urls
            );

            // Convertir gráfico a imagen
            byte[] imageBytes = EncoderUtil.encode(pieChart.createBufferedImage(400, 300), ImageFormat.PNG);

            // Insertar imagen en el sheet
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(startCol);
            anchor.setCol2(startCol + 12);
            anchor.setRow1(startRow);
            anchor.setRow2(startRow + 18);

            drawing.createPicture(anchor, workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG));
        } catch (Exception e) {
            throw new RuntimeException("Error creating pie chart: " + title, e);
        }
    }

    private void createBarChart(XSSFWorkbook workbook, XSSFSheet sheet, String title, List<Map<String, ?>> data, int startRow, int startCol) {
        if (data == null || data.isEmpty()) return;

        try {
            // Crear dataset para el gráfico de barras
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Map<String, ?> row : data) {
                String category = row.get("category") != null ? row.get("category").toString() : 
                                (row.get("label") != null ? row.get("label").toString() : "");
                Object val = row.get("value");
                double value = 0;
                if (val instanceof Number) {
                    value = ((Number) val).doubleValue();
                } else if (val != null) {
                    try {
                        value = Double.parseDouble(val.toString());
                    } catch (NumberFormatException e) {
                        value = 0;
                    }
                }
                dataset.addValue(value, "Values", category);
            }

            // Crear gráfico de barras
            JFreeChart barChart = ChartFactory.createBarChart(
                title,
                "Category",
                "Value",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,  // legend
                false, // tooltips
                false  // urls
            );

            // Convertir gráfico a imagen
            byte[] imageBytes = EncoderUtil.encode(barChart.createBufferedImage(400, 300), ImageFormat.PNG);

            // Insertar imagen en el sheet
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(startCol);
            anchor.setCol2(startCol + 12);
            anchor.setRow1(startRow);
            anchor.setRow2(startRow + 18);

            drawing.createPicture(anchor, workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG));
        } catch (Exception e) {
            throw new RuntimeException("Error creating bar chart: " + title, e);
        }
    }

    private XSSFCellStyle createTitleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 28);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x00, (byte) 0x66, (byte) 0x99}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle createSubtitleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 14);
        font.setColor(new XSSFColor(new byte[]{(byte) 0x66, (byte) 0x66, (byte) 0x66}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x00, (byte) 0x66, (byte) 0x99}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle createDataStyle(XSSFWorkbook workbook, boolean alternate) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        if (alternate) {
            style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0xF0, (byte) 0xEF, (byte) 0xEF}, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private XSSFCellStyle createFooterStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xD0, (byte) 0xB4, (byte) 0x8E}));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x00, (byte) 0x66, (byte) 0x99}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        String normalized = str.toLowerCase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("ñ", "n").replace("ü", "u");
        String[] words = normalized.split("\\s+");
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(words[i].substring(0, 1).toUpperCase()).append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
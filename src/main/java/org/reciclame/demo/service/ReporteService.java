package org.reciclame.demo.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final short HEADER_BG = 0x006699;

    public byte[] generarReport(String reporte) {
        try {
            InputStream reporteStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jasper");
            JasperPrint jasperPrint = JasperFillManager.fillReport(reporteStream, new java.util.HashMap<>(), new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException e) {
            throw new RuntimeException("Error generating report: " + reporte, e);
        }
    }

    public byte[] generarReportePDF(String reporte, JRDataSource dataSource, Map<String, Object> params) {
        try {
            InputStream reporteStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jasper");
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reporteStream);
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
            InputStream reporteStream = this.getClass().getResourceAsStream("/reports/" + reporte + ".jasper");
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reporteStream);

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
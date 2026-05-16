package com.cinescoop.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;

public class MockExcelGenerator {
    private static final Logger logger = LoggerFactory.getLogger(MockExcelGenerator.class);

    public static void generateMockExcel(String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Movies");

            // Header
            Row header = sheet.createRow(0);
            String[] headers = {"ID", "Title", "Genre", "Year", "Duration", "Language", "Country", "Budget", "Revenue", "Rating", "DirectorID", "Company", "AgeLimit", "Status"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Row 1: Good data
            createRow(sheet, 1, "1", "Inception", "Sci-Fi", "2010", "148", "English", "USA", "160000000", "825532764", "8.8", "1", "Warner Bros", "13", "Active");

            // Row 2: Corrupted Data (Spaces, lowercase, bad email concept, negative duration, bad budget text)
            createRow(sheet, 2, "2", "   the matrix  ", "badword sci-fi", "1999", "-136", " EN", "us", "$63,000,000", "463517383", "15", "1", "<p>Warner</p>", "500", "Actf");

            // Row 3: Nulls and weird types
            createRow(sheet, 3, "3", "", "Action", "Two Thousand", "120", "English", "uk", "10000", "50000", "-5", "2", "Universal", "18", "inactive");

            // We generate 1000 rows by looping and injecting random issues
            for (int i = 4; i <= 1000; i++) {
                createRow(sheet, i, 
                        String.valueOf(i), 
                        "Movie " + i + "   ", 
                        (i % 2 == 0) ? "Action" : "spam drama", 
                        "202" + (i%10), 
                        String.valueOf(90 + (i%30)), 
                        "English", 
                        (i % 3 == 0) ? "USA" : "France", 
                        "$" + (1000000 + i*1000), 
                        String.valueOf(5000000 + i*5000), 
                        String.valueOf((i%10) + 2), 
                        "1", 
                        "Studio " + i, 
                        "13", 
                        "Active"
                );
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
                logger.info("Created Mock Excel file at: {}", filePath);
            }

        } catch (Exception e) {
            logger.error("Failed to create mock Excel file: {}", e.getMessage());
        }
    }

    private static void createRow(Sheet sheet, int rowIdx, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }
}

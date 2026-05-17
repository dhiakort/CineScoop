package com.cinescoop.main;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.database.DatabaseInitializer;
import com.cinescoop.service.ExcelImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MainApp — ETL entry point for CineScoop AI.
 * Process: Create Database Schema -> Read Excel -> Clean Data (20 solutions) -> Insert clean data to MySQL.
 */
public class MainApp {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        logger.info("═══════════════════════════════════════");
        logger.info("   CineScoop AI  —  ETL Pipeline       ");
        logger.info("═══════════════════════════════════════");

        // 1. Verify DB is reachable
        boolean dbConnected = DatabaseConnection.getInstance().testConnection();
        if (!dbConnected) {
            logger.error("Cannot reach MySQL. Check db.properties and make sure the server is running.");
            return;
        }

        // 2. Create tables if they do not exist yet
        DatabaseInitializer.initialize();
        logger.info(DatabaseConnection.getInstance().getPoolStats());

        // 3. Run ETL Process in correct order (Parents -> Children)
        logger.info("Starting ETL Pipeline — Parent Tables...");
        com.cinescoop.service.CsvImportService csvImporter = new com.cinescoop.service.CsvImportService();
        
        // Step A: Import Directors, Actors, Users from CSV
        // (These are parent tables needed by Movies and Ratings)
        csvImporter.importParents(System.getProperty("user.dir"));

        logger.info("Starting Movie Extraction and Cleaning (Excel)...");
        String excelFilePath = "movies_data_corrupted.xlsx"; 
        java.io.File file = new java.io.File(excelFilePath);
        if (!file.exists()) {
            com.cinescoop.service.MockExcelGenerator.generateMockExcel(excelFilePath);
        }

        ExcelImportService excelService = new ExcelImportService();
        // Step B: Import Movies from Excel (points to Directors)
        excelService.importFromExcel(excelFilePath);

        logger.info("Starting Relational Data Import (Ratings & MovieActors)...");
        // Step C: Import Ratings and MovieActors from CSV (points to Movies, Users, Actors)
        csvImporter.importChildren(System.getProperty("user.dir"));

        logger.info("ETL Process Complete. You can now connect Power BI to the MySQL database!");

        // Graceful shutdown
        DatabaseConnection.getInstance().shutdown();
    }
}
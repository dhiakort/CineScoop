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

        // 3. Run Excel Import and Data Cleaning (Solutions)
        logger.info("Starting Data Extraction and Cleaning process...");
        String excelFilePath = "movies_data_corrupted.xlsx"; 
        
        // Generate the mock Excel file with 1000 corrupted rows if it doesn't exist
        java.io.File file = new java.io.File(excelFilePath);
        if (!file.exists()) {
            com.cinescoop.service.MockExcelGenerator.generateMockExcel(excelFilePath);
        }

        ExcelImportService excelService = new ExcelImportService();
        // Run the process: Read corrupted Excel -> Clean data -> Insert into MySQL
        excelService.importFromExcel(excelFilePath);

        logger.info("Importing remaining relational data from CSVs (Users, Actors, Ratings)...");
        com.cinescoop.service.CsvImportService csvImporter = new com.cinescoop.service.CsvImportService();
        csvImporter.importAll(System.getProperty("user.dir"));

        logger.info("ETL Process Complete. You can now connect Power BI to the MySQL database to generate the dashboard!");

        // Graceful shutdown
        DatabaseConnection.getInstance().shutdown();
    }
}
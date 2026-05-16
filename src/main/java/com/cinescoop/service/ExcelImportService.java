package com.cinescoop.service;

import com.cinescoop.dao.MovieDAO;
import com.cinescoop.model.Movie;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelImportService — reads the Excel file with 1000 rows and 20 problems,
 * applies the 20 DataCleaner solutions, and saves the cleaned data to MySQL.
 */
public class ExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);
    private final DataCleaner cleaner = new DataCleaner();
    private final MovieDAO movieDAO = new MovieDAO();

    public void importFromExcel(String filePath) {
        logger.info("Starting Excel Import from {}", filePath);
        List<Movie> validMovies = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // Skip header row
                if (row.getRowNum() == 0) continue;

                try {
                    Movie m = new Movie();

                    // Apply solutions on data fields:
                    String idStr = getCellString(row, 0);
                    m.setMovieId(cleaner.absoluteValue(cleaner.parseSafeInt(idStr, 0))); // absoluteValue + safeInt

                    String titleRaw = getCellString(row, 1);
                    m.setTitle(cleaner.collapseSpaces(cleaner.capitalizeName(cleaner.handleNullString(titleRaw))));

                    String genreRaw = getCellString(row, 2);
                    m.setGenre(cleaner.filterProfanity(cleaner.trimWhitespace(genreRaw)));

                    String yearStr = getCellString(row, 3);
                    m.setReleaseYear(cleaner.parseSafeInt(yearStr, 2024));

                    String durationStr = getCellString(row, 4);
                    m.setDuration(cleaner.absoluteValue(cleaner.parseSafeInt(durationStr, 120)));

                    String lang = getCellString(row, 5);
                    m.setLanguage(cleaner.limitLength(cleaner.trimWhitespace(lang), 50));

                    String countryRaw = getCellString(row, 6);
                    m.setCountry(cleaner.standardizeCountry(countryRaw));

                    String budgetRaw = getCellString(row, 7);
                    m.setBudget(cleaner.parseSafeBigDecimal(budgetRaw));

                    String revenueRaw = getCellString(row, 8);
                    m.setRevenue(cleaner.parseSafeBigDecimal(revenueRaw));

                    String ratingRaw = getCellString(row, 9);
                    float rating = cleaner.parseSafeInt(ratingRaw, 0);
                    m.setRatingAverage(cleaner.capRating(rating));

                    String directorIdRaw = getCellString(row, 10);
                    m.setDirectorId(cleaner.parseSafeInt(directorIdRaw, 1));

                    String companyRaw = getCellString(row, 11);
                    m.setProductionCompany(cleaner.removeHtmlTags(cleaner.trimWhitespace(companyRaw)));

                    String ageRaw = getCellString(row, 12);
                    m.setAgeLimit(cleaner.validateAge(cleaner.parseSafeInt(ageRaw, 0)));

                    String statusRaw = getCellString(row, 13);
                    m.setStatus(cleaner.normalizeMovieStatus(statusRaw));

                    // Add only valid cleaned records
                    if (m.getMovieId() > 0 && !m.getTitle().equals("Unknown")) {
                        validMovies.add(m);
                    }

                } catch (Exception e) {
                    logger.warn("Skipped row {} due to severe corruption: {}", row.getRowNum(), e.getMessage());
                }
            }

            logger.info("Excel reading complete. Saving {} cleaned movies to the database...", validMovies.size());
            movieDAO.insertBatch(validMovies);
            logger.info("Database insertion complete!");

        } catch (Exception e) {
            logger.error("Error processing Excel file: {}", e.getMessage(), e);
        }
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return cell.toString();
        }
    }
}

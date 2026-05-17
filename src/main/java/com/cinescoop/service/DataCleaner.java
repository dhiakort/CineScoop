package com.cinescoop.service;

import org.apache.commons.lang3.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * DataCleaner — provides 20 distinct data cleaning solutions to handle common data quality issues.
 */
public class DataCleaner {

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Solution 1: Trim Whitespace
    public String trimWhitespace(String value) {
        return value == null ? "" : value.trim();
    }

    // Solution 2: Replace Null with Unknown
    public String handleNullString(String value) {
        return (value == null || value.trim().isEmpty()) ? "Unknown" : value;
    }

    // Solution 3: Capitalize Name
    public String capitalizeName(String name) {
        if (name == null || name.isEmpty()) return "Unknown";
        return StringUtils.capitalize(name.toLowerCase());
    }

    // Solution 4: Validate and Clean Email
    public String cleanEmail(String email) {
        if (email == null) return "no-reply@cinescoop.com";
        email = email.trim().toLowerCase();
        if (email.contains("@") && email.contains(".")) return email;
        return "invalid@cinescoop.com";
    }

    // Solution 5: Extract Numbers only (Remove text from numeric fields like "$1,000")
    public String removeNonNumeric(String value) {
        if (value == null) return "0";
        return value.replaceAll("[^\\d.-]", "");
    }

    // Solution 6: Safe Integer Parse with Default
    public int parseSafeInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(removeNonNumeric(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Solution 7: Fix Negative Values (Convert to Absolute)
    public int absoluteValue(int value) {
        return Math.abs(value);
    }

    // Solution 8: Validate Age Range
    public int validateAge(int age) {
        if (age < 0 || age > 120) return 0; // 0 means unknown
        return age;
    }

    // Solution 9: Normalize Gender (M/F/Other)
    public String normalizeGender(String gender) {
        if (gender == null) return "Other";
        String g = gender.trim().toUpperCase();
        if (g.startsWith("M")) return "M";
        if (g.startsWith("F")) return "F";
        return "Other";
    }

    // Solution 10: Normalize Booleans (1/0, Yes/No, True/False)
    public boolean parseBoolean(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y");
    }

    // Solution 11: Format Dates consistently
    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return LocalDate.now();
        try {
            // handle DD/MM/YYYY to YYYY-MM-DD
            if (dateStr.contains("/")) {
                String[] parts = dateStr.split("/");
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            return LocalDate.parse(dateStr, D_FMT);
        } catch (Exception e) {
            return LocalDate.now(); // Fallback
        }
    }

    // Solution 12: Remove HTML Tags from text
    public String removeHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "");
    }

    // Solution 13: Limit String length (e.g., for DB columns)
    public String limitLength(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    // Solution 14: Normalize Status for User/Director (Active/Inactive)
    public String normalizeStatus(String status) {
        if (status == null) return "Inactive";
        String s = status.trim().toLowerCase();
        return s.equals("active") ? "Active" : "Inactive";
    }

    // Solution 14b: Normalize Status for Movie (Released/Production/Cancelled)
    public String normalizeMovieStatus(String status) {
        if (status == null) return "Released";
        String s = status.trim().toLowerCase();
        if (s.contains("prod")) return "Production";
        if (s.contains("cancel")) return "Cancelled";
        if (s.contains("rel") || s.contains("act")) return "Released";
        return "Released";
    }

    // Solution 15: Clean Phone Numbers (Only Digits and +)
    public String cleanPhone(String phone) {
        if (phone == null) return "00000000";
        String p = phone.replaceAll("[^\\d+]", "");
        return p.isEmpty() ? "00000000" : p;
    }

    // Solution 16: Safe BigDecimal parsing
    public BigDecimal parseSafeBigDecimal(String value) {
        try {
            return new BigDecimal(removeNonNumeric(value));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // Solution 17: Cap maximum values (e.g., Rating out of 10)
    public float capRating(float rating) {
        if (rating < 0) return 0f;
        if (rating > 10) return 10f;
        return rating;
    }

    // Solution 18: Standardize Country Names
    public String standardizeCountry(String country) {
        if (country == null) return "Unknown";
        String c = country.trim().toUpperCase();
        if (c.equals("US") || c.equals("USA")) return "United States";
        if (c.equals("UK") || c.equals("ENGLAND")) return "United Kingdom";
        if (c.equals("FR")) return "France";
        return StringUtils.capitalize(country.toLowerCase());
    }

    // Solution 19: Remove multiple spaces
    public String collapseSpaces(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    // Solution 20: Replace specific banned words
    public String filterProfanity(String text) {
        if (text == null) return "";
        return text.replaceAll("(?i)\\b(badword|ugly|spam)\\b", "***");
    }
}

/**
 * Income Record Data Model - Alternative Implementation
 * Represents income transaction with validation capabilities
 */

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

public class IncomeRecord {

    // Core Data Fields
    private String code;
    private String description;
    private String incomeDate;
    private double incomeAmount;
    private double whtAmount;
    private int originalChecksum;
    private int calculatedChecksum;
    private boolean valid;

    // Validation Patterns
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{2}\\d{3}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Business Constants
    private static final int MAX_DESCRIPTION_LENGTH = 20;

    // Constructors
    public IncomeRecord() {
        this.valid = false;
    }

    public IncomeRecord(String code, String description, String incomeDate,
                        double incomeAmount, double whtAmount) {
        setCode(code);
        setDescription(description);
        setIncomeDate(incomeDate);
        setIncomeAmount(incomeAmount);
        setWhtAmount(whtAmount);
        this.originalChecksum = 0;
        this.calculatedChecksum = 0;
        this.valid = false;
    }

    public IncomeRecord(String code, String description, String incomeDate,
                        double incomeAmount, double whtAmount, int originalChecksum) {
        this(code, description, incomeDate, incomeAmount, whtAmount);
        this.originalChecksum = originalChecksum;
    }

    // Validation Methods
    public static boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        String normalizedCode = code.trim().toUpperCase();
        return CODE_PATTERN.matcher(normalizedCode).matches();
    }

    public static boolean isValidDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }
        return description.trim().length() <= MAX_DESCRIPTION_LENGTH;
    }

    public static boolean isValidDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }

        if (!DATE_PATTERN.matcher(dateStr.trim()).matches()) {
            return false;
        }

        try {
            LocalDate.parse(dateStr.trim(), DATE_FORMAT);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidIncomeAmount(double amount) {
        return amount > 0;
    }

    public static boolean isValidWhtAmount(double amount) {
        return amount >= 0;
    }

    // Business Logic Methods
    public double getNetIncome() {
        return Math.max(0, incomeAmount - whtAmount);
    }

    public String getFormattedIncome() {
        return String.format("Rs %.2f", incomeAmount);
    }

    public String getFormattedWht() {
        return String.format("Rs %.2f", whtAmount);
    }

    public String getFormattedNet() {
        return String.format("Rs %.2f", getNetIncome());
    }

    public boolean hasValidFormat() {
        return isValidCode(code) &&
                isValidDescription(description) &&
                isValidDate(incomeDate) &&
                isValidIncomeAmount(incomeAmount) &&
                isValidWhtAmount(whtAmount);
    }

    // Data Conversion Methods
    public String toCSVFormat() {
        return String.format("%s,%s,%s,%.2f,%.2f,%d",
                code, description, incomeDate, incomeAmount, whtAmount, calculatedChecksum);
    }

    public String toDataFormat() {
        return String.format("%s|%s|%s|%.2f|%.2f",
                code, description, incomeDate, incomeAmount, whtAmount);
    }

    public static IncomeRecord fromCSVFormat(String csvLine) {
        if (csvLine == null || csvLine.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV line cannot be empty");
        }

        String[] parts = parseCSVLine(csvLine);

        if (parts.length < 5) {
            throw new IllegalArgumentException("Insufficient CSV data fields");
        }

        try {
            String code = parts[0].trim();
            String description = parts[1].trim();
            String date = parts[2].trim();
            double income = Double.parseDouble(parts[3].trim());
            double wht = Double.parseDouble(parts[4].trim());

            int checksum = 0;
            if (parts.length >= 6 && !parts[5].trim().isEmpty()) {
                checksum = Integer.parseInt(parts[5].trim());
            }

            return new IncomeRecord(code, description, date, income, wht, checksum);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric data in CSV: " + e.getMessage());
        }
    }

    private static String[] parseCSVLine(String line) {
        // Simple CSV parser - handles basic comma separation
        return line.split(",");
    }

    public static IncomeRecord fromDataFormat(String dataLine) {
        if (dataLine == null || dataLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Data line cannot be empty");
        }

        String[] parts = dataLine.trim().split("\\|");

        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid data line format");
        }

        try {
            String code = parts[0].trim();
            String description = parts[1].trim();
            String date = parts[2].trim();
            double income = Double.parseDouble(parts[3].trim());
            double wht = Double.parseDouble(parts[4].trim());

            return new IncomeRecord(code, description, date, income, wht);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric data: " + e.getMessage());
        }
    }

    // Update Methods
    public void updateData(String description, String incomeDate, double incomeAmount, double whtAmount) {
        setDescription(description);
        setIncomeDate(incomeDate);
        setIncomeAmount(incomeAmount);
        setWhtAmount(whtAmount);
        this.valid = false; // Reset validation status
    }

    public IncomeRecord createCopy() {
        IncomeRecord copy = new IncomeRecord(code, description, incomeDate, incomeAmount, whtAmount, originalChecksum);
        copy.setCalculatedChecksum(calculatedChecksum);
        copy.setValid(valid);
        return copy;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        if (!isValidCode(code)) {
            throw new IllegalArgumentException("Invalid income code format. Must be 2 letters + 3 digits (e.g., IN001)");
        }
        this.code = code.trim().toUpperCase();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (!isValidDescription(description)) {
            throw new IllegalArgumentException("Description must be 1-20 characters long");
        }
        this.description = description.trim();
    }

    public String getIncomeDate() {
        return incomeDate;
    }

    public void setIncomeDate(String incomeDate) {
        if (!isValidDate(incomeDate)) {
            throw new IllegalArgumentException("Date must be in DD/MM/YYYY format and valid");
        }
        this.incomeDate = incomeDate.trim();
    }

    public double getIncomeAmount() {
        return incomeAmount;
    }

    public void setIncomeAmount(double incomeAmount) {
        if (!isValidIncomeAmount(incomeAmount)) {
            throw new IllegalArgumentException("Income amount must be positive");
        }
        this.incomeAmount = Math.round(incomeAmount * 100.0) / 100.0;
    }

    public double getWhtAmount() {
        return whtAmount;
    }

    public void setWhtAmount(double whtAmount) {
        if (!isValidWhtAmount(whtAmount)) {
            throw new IllegalArgumentException("WHT amount cannot be negative");
        }
        this.whtAmount = Math.round(whtAmount * 100.0) / 100.0;
    }

    public int getOriginalChecksum() {
        return originalChecksum;
    }

    public void setOriginalChecksum(int originalChecksum) {
        this.originalChecksum = originalChecksum;
    }

    public int getCalculatedChecksum() {
        return calculatedChecksum;
    }

    public void setCalculatedChecksum(int calculatedChecksum) {
        this.calculatedChecksum = calculatedChecksum;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    // Object Methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        IncomeRecord that = (IncomeRecord) obj;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return String.format("IncomeRecord{code='%s', description='%s', date='%s', " +
                        "income=%.2f, wht=%.2f, net=%.2f, valid=%b}",
                code, description, incomeDate, incomeAmount, whtAmount,
                getNetIncome(), valid);
    }

    public String toDetailedString() {
        return String.format("Income Record Details:\n" +
                        "  Code: %s\n" +
                        "  Description: %s\n" +
                        "  Date: %s\n" +
                        "  Income Amount: Rs %.2f\n" +
                        "  WHT Amount: Rs %.2f\n" +
                        "  Net Income: Rs %.2f\n" +
                        "  Original Checksum: %d\n" +
                        "  Calculated Checksum: %d\n" +
                        "  Status: %s",
                code, description, incomeDate, incomeAmount, whtAmount,
                getNetIncome(), originalChecksum, calculatedChecksum,
                valid ? "Valid" : "Invalid");
    }

    // Comparison Methods
    public int compareByCode(IncomeRecord other) {
        return this.code.compareTo(other.code);
    }

    public int compareByDate(IncomeRecord other) {
        return this.incomeDate.compareTo(other.incomeDate);
    }

    public int compareByAmount(IncomeRecord other) {
        return Double.compare(this.incomeAmount, other.incomeAmount);
    }

    public int compareByDescription(IncomeRecord other) {
        return this.description.compareTo(other.description);
    }
}
/**
 * Validation Engine - Alternative Implementation
 * Comprehensive validation system for income records
 */

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ValidationEngine {

    // Validation Statistics
    private final AtomicInteger validationCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    // Constructor
    public ValidationEngine() {
        // Initialize validation engine
    }

    /**
     * Validate a single income record
     *
     * @param record The record to validate
     * @return true if valid, false otherwise
     */
    public boolean validateRecord(IncomeRecord record) {
        if (record == null) {
            System.err.println("Cannot validate null record");
            return false;
        }

        validationCount.incrementAndGet();

        try {
            // Step 1: Format validation
            if (!validateRecordFormat(record)) {
                record.setValid(false);
                failureCount.incrementAndGet();
                return false;
            }

            // Step 2: Business rule validation
            if (!validateBusinessRules(record)) {
                record.setValid(false);
                failureCount.incrementAndGet();
                return false;
            }

            // Step 3: Checksum validation
            int calculatedChecksum = calculateChecksum(record);
            record.setCalculatedChecksum(calculatedChecksum);

            boolean checksumValid = (calculatedChecksum == record.getOriginalChecksum());
            record.setValid(checksumValid);

            if (checksumValid) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }

            return checksumValid;

        } catch (Exception e) {
            System.err.println("Validation error for record " + record.getCode() + ": " + e.getMessage());
            record.setValid(false);
            failureCount.incrementAndGet();
            return false;
        }
    }

    /**
     * Validate record format and basic constraints
     *
     * @param record Record to validate
     * @return true if format is valid
     */
    private boolean validateRecordFormat(IncomeRecord record) {
        List<String> errors = new ArrayList<>();

        // Validate income code
        if (!IncomeRecord.isValidCode(record.getCode())) {
            errors.add("Invalid income code format");
        }

        // Validate description
        if (!IncomeRecord.isValidDescription(record.getDescription())) {
            errors.add("Invalid description");
        }

        // Validate date
        if (!IncomeRecord.isValidDate(record.getIncomeDate())) {
            errors.add("Invalid date format");
        }

        // Validate amounts
        if (!IncomeRecord.isValidIncomeAmount(record.getIncomeAmount())) {
            errors.add("Invalid income amount");
        }

        if (!IncomeRecord.isValidWhtAmount(record.getWhtAmount())) {
            errors.add("Invalid WHT amount");
        }

        if (!errors.isEmpty()) {
            System.err.println("Format validation failed for " + record.getCode() + ": " +
                    String.join(", ", errors));
            return false;
        }

        return true;
    }

    /**
     * Validate business rules
     *
     * @param record Record to validate
     * @return true if business rules are satisfied
     */
    private boolean validateBusinessRules(IncomeRecord record) {
        List<String> warnings = new ArrayList<>();

        // Rule 1: WHT should not exceed income
        if (record.getWhtAmount() > record.getIncomeAmount()) {
            warnings.add("WHT amount exceeds income amount");
        }

        // Rule 2: Very high income amounts (potential data entry error)
        if (record.getIncomeAmount() > 10000000) { // 10 million
            warnings.add("Extremely high income amount detected");
        }

        // Rule 3: Very old dates (potential data entry error)
        String dateStr = record.getIncomeDate();
        if (dateStr != null && dateStr.length() >= 4) {
            try {
                int year = Integer.parseInt(dateStr.substring(6, 10));
                int currentYear = java.time.LocalDate.now().getYear();
                if (year < (currentYear - 10)) {
                    warnings.add("Income date is more than 10 years old");
                }
            } catch (Exception e) {
                // Ignore parsing errors (already caught in format validation)
            }
        }

        // Log warnings but don't fail validation
        if (!warnings.isEmpty()) {
            System.out.println("Business rule warnings for " + record.getCode() + ": " +
                    String.join(", ", warnings));
        }

        return true; // Business rules are warnings, not failures
    }

    /**
     * Calculate checksum for income record using the specified algorithm
     *
     * Algorithm:
     * 1. Create transaction line without checksum
     * 2. Count capital letters
     * 3. Count numbers and decimal points
     * 4. Sum the counts
     *
     * @param record Income record
     * @return Calculated checksum
     */
    public int calculateChecksum(IncomeRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }

        // Create transaction line without checksum
        String transactionLine = String.format("%s,%s,%s,%.2f,%.2f",
                record.getCode(),
                record.getDescription(),
                record.getIncomeDate(),
                record.getIncomeAmount(),
                record.getWhtAmount()
        );

        return calculateChecksumFromString(transactionLine);
    }

    /**
     * Calculate checksum from string
     *
     * @param input Input string
     * @return Calculated checksum
     */
    public int calculateChecksumFromString(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }

        int capitalLetters = 0;
        int numbersAndDecimals = 0;

        for (char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                capitalLetters++;
            } else if (Character.isDigit(c) || c == '.') {
                numbersAndDecimals++;
            }
        }

        return capitalLetters + numbersAndDecimals;
    }

    /**
     * Batch validate multiple records
     *
     * @param records List of records to validate
     * @return Validation summary
     */
    public ValidationSummary validateRecords(List<IncomeRecord> records) {
        ValidationSummary summary = new ValidationSummary();

        if (records == null || records.isEmpty()) {
            return summary;
        }

        System.out.println("Starting batch validation of " + records.size() + " records...");

        for (IncomeRecord record : records) {
            boolean isValid = validateRecord(record);

            summary.incrementTotal();
            if (isValid) {
                summary.addValidRecord(record);
            } else {
                summary.addInvalidRecord(record);
            }
        }

        System.out.println("Batch validation completed:");
        System.out.println("  Total: " + summary.getTotalCount());
        System.out.println("  Valid: " + summary.getValidCount());
        System.out.println("  Invalid: " + summary.getInvalidCount());
        System.out.println("  Success Rate: " + String.format("%.1f%%", summary.getSuccessRate()));

        return summary;
    }

    /**
     * Recalculate checksums for all records
     *
     * @param records Records to process
     * @return Number of records processed
     */
    public int recalculateChecksums(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int processedCount = 0;

        for (IncomeRecord record : records) {
            try {
                int newChecksum = calculateChecksum(record);
                record.setCalculatedChecksum(newChecksum);
                processedCount++;
            } catch (Exception e) {
                System.err.println("Error recalculating checksum for " + record.getCode() + ": " + e.getMessage());
            }
        }

        System.out.println("Recalculated checksums for " + processedCount + " records");
        return processedCount;
    }

    /**
     * Fix invalid records by updating their checksums
     *
     * @param records Records to fix
     * @return Number of records fixed
     */
    public int repairInvalidRecords(List<IncomeRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int repairedCount = 0;

        for (IncomeRecord record : records) {
            if (!record.isValid()) {
                try {
                    int calculatedChecksum = calculateChecksum(record);
                    record.setOriginalChecksum(calculatedChecksum);
                    record.setCalculatedChecksum(calculatedChecksum);
                    record.setValid(true);
                    repairedCount++;
                } catch (Exception e) {
                    System.err.println("Error repairing record " + record.getCode() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Repaired " + repairedCount + " invalid records");
        return repairedCount;
    }

    /**
     * Generate detailed validation report for a record
     *
     * @param record Record to analyze
     * @return Detailed validation report
     */
    public ValidationReport generateDetailedReport(IncomeRecord record) {
        ValidationReport report = new ValidationReport(record);

        if (record == null) {
            report.addError("Record is null");
            return report;
        }

        // Format validation
        if (!validateRecordFormat(record)) {
            report.addError("Record format validation failed");
        }

        // Business rule validation
        validateBusinessRules(record); // Warnings are logged internally

        // Checksum analysis
        try {
            String transactionLine = String.format("%s,%s,%s,%.2f,%.2f",
                    record.getCode(), record.getDescription(), record.getIncomeDate(),
                    record.getIncomeAmount(), record.getWhtAmount());

            int calculatedChecksum = calculateChecksumFromString(transactionLine);

            report.setTransactionLine(transactionLine);
            report.setCalculatedChecksum(calculatedChecksum);
            report.setOriginalChecksum(record.getOriginalChecksum());

            // Detailed analysis
            int capitals = 0, numbersDecimals = 0;
            for (char c : transactionLine.toCharArray()) {
                if (Character.isUpperCase(c)) capitals++;
                else if (Character.isDigit(c) || c == '.') numbersDecimals++;
            }

            report.setCapitalCount(capitals);
            report.setNumberDecimalCount(numbersDecimals);

            boolean valid = (calculatedChecksum == record.getOriginalChecksum());
            report.setValid(valid);

            if (!valid) {
                report.addError("Checksum mismatch: expected " + record.getOriginalChecksum() +
                        ", calculated " + calculatedChecksum);
            }

        } catch (Exception e) {
            report.addError("Error during checksum analysis: " + e.getMessage());
        }

        return report;
    }

    /**
     * Test the checksum algorithm with known values
     *
     * @return Test results
     */
    public ChecksumTestResult testChecksumAlgorithm() {
        ChecksumTestResult result = new ChecksumTestResult();

        // Test case 1: Known value
        String testLine1 = "IN001,Freelance Work,25/07/2025,10000.00,1000.00";
        int calculated1 = calculateChecksumFromString(testLine1);
        int expected1 = 30; // Expected checksum

        result.addTest("Basic Test", testLine1, expected1, calculated1);

        // Test case 2: Different record
        String testLine2 = "SA002,Consulting,26/07/2025,15000.00,1500.00";
        int calculated2 = calculateChecksumFromString(testLine2);

        result.addTest("Variation Test", testLine2, -1, calculated2); // No expected value

        // Test case 3: Edge case
        String testLine3 = "AB123,Test,01/01/2024,1.00,0.00";
        int calculated3 = calculateChecksumFromString(testLine3);

        result.addTest("Edge Case", testLine3, -1, calculated3);

        return result;
    }

    /**
     * Get validation statistics
     *
     * @return Statistics summary
     */
    public ValidationStatistics getStatistics() {
        return new ValidationStatistics(
                validationCount.get(),
                successCount.get(),
                failureCount.get()
        );
    }

    /**
     * Reset validation statistics
     */
    public void resetStatistics() {
        validationCount.set(0);
        successCount.set(0);
        failureCount.set(0);
    }

    // Helper Classes
    public static class ValidationSummary {
        private int totalCount = 0;
        private final List<IncomeRecord> validRecords = new ArrayList<>();
        private final List<IncomeRecord> invalidRecords = new ArrayList<>();

        public void incrementTotal() { totalCount++; }
        public void addValidRecord(IncomeRecord record) { validRecords.add(record); }
        public void addInvalidRecord(IncomeRecord record) { invalidRecords.add(record); }

        public int getTotalCount() { return totalCount; }
        public int getValidCount() { return validRecords.size(); }
        public int getInvalidCount() { return invalidRecords.size(); }
        public List<IncomeRecord> getValidRecords() { return new ArrayList<>(validRecords); }
        public List<IncomeRecord> getInvalidRecords() { return new ArrayList<>(invalidRecords); }

        public double getSuccessRate() {
            return totalCount > 0 ? (double) validRecords.size() / totalCount * 100.0 : 0.0;
        }
    }

    public static class ValidationReport {
        private final IncomeRecord record;
        private String transactionLine;
        private int originalChecksum;
        private int calculatedChecksum;
        private int capitalCount;
        private int numberDecimalCount;
        private boolean valid;
        private final List<String> errors = new ArrayList<>();

        public ValidationReport(IncomeRecord record) {
            this.record = record;
        }

        // Getters and setters
        public IncomeRecord getRecord() { return record; }
        public String getTransactionLine() { return transactionLine; }
        public void setTransactionLine(String transactionLine) { this.transactionLine = transactionLine; }
        public int getOriginalChecksum() { return originalChecksum; }
        public void setOriginalChecksum(int originalChecksum) { this.originalChecksum = originalChecksum; }
        public int getCalculatedChecksum() { return calculatedChecksum; }
        public void setCalculatedChecksum(int calculatedChecksum) { this.calculatedChecksum = calculatedChecksum; }
        public int getCapitalCount() { return capitalCount; }
        public void setCapitalCount(int capitalCount) { this.capitalCount = capitalCount; }
        public int getNumberDecimalCount() { return numberDecimalCount; }
        public void setNumberDecimalCount(int numberDecimalCount) { this.numberDecimalCount = numberDecimalCount; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public void addError(String error) { errors.add(error); }
    }

    public static class ChecksumTestResult {
        private final List<TestCase> testCases = new ArrayList<>();

        public void addTest(String name, String input, int expected, int calculated) {
            testCases.add(new TestCase(name, input, expected, calculated));
        }

        public List<TestCase> getTestCases() { return new ArrayList<>(testCases); }

        public boolean allTestsPassed() {
            return testCases.stream().allMatch(TestCase::isPassed);
        }

        public static class TestCase {
            private final String name;
            private final String input;
            private final int expected;
            private final int calculated;

            public TestCase(String name, String input, int expected, int calculated) {
                this.name = name;
                this.input = input;
                this.expected = expected;
                this.calculated = calculated;
            }

            public boolean isPassed() {
                return expected == -1 || expected == calculated; // -1 means no expected value
            }

            public String getName() { return name; }
            public String getInput() { return input; }
            public int getExpected() { return expected; }
            public int getCalculated() { return calculated; }
        }
    }

    public static class ValidationStatistics {
        private final int totalValidations;
        private final int successfulValidations;
        private final int failedValidations;

        public ValidationStatistics(int total, int successful, int failed) {
            this.totalValidations = total;
            this.successfulValidations = successful;
            this.failedValidations = failed;
        }

        public int getTotalValidations() { return totalValidations; }
        public int getSuccessfulValidations() { return successfulValidations; }
        public int getFailedValidations() { return failedValidations; }

        public double getSuccessRate() {
            return totalValidations > 0 ? (double) successfulValidations / totalValidations * 100.0 : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ValidationStats{total=%d, success=%d, failed=%d, rate=%.1f%%}",
                    totalValidations, successfulValidations, failedValidations, getSuccessRate());
        }
    }
}
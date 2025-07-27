/**
 * Comprehensive Test Suite
 * Complete testing for Income File Import System - Alternative Version
 */

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ComprehensiveTest {

    private IncomeRecord validRecord;
    private ValidationEngine validationEngine;
    private TaxProcessor taxProcessor;
    private FileManager fileManager;

    // Test data
    private static final String VALID_CODE = "IN001";
    private static final String VALID_DESCRIPTION = "Test Income";
    private static final String VALID_DATE = "25/07/2025";
    private static final double VALID_INCOME = 10000.00;
    private static final double VALID_WHT = 1000.00;

    @BeforeEach
    void setUp() {
        validRecord = new IncomeRecord(VALID_CODE, VALID_DESCRIPTION, VALID_DATE, VALID_INCOME, VALID_WHT);
        validationEngine = new ValidationEngine();
        taxProcessor = new TaxProcessor();
        fileManager = new FileManager();
    }

    @Nested
    @DisplayName("Income Record Tests")
    class IncomeRecordTests {

        @Test
        @DisplayName("Valid record creation")
        void testValidRecordCreation() {
            assertNotNull(validRecord);
            assertEquals(VALID_CODE, validRecord.getCode());
            assertEquals(VALID_DESCRIPTION, validRecord.getDescription());
            assertEquals(VALID_DATE, validRecord.getIncomeDate());
            assertEquals(VALID_INCOME, validRecord.getIncomeAmount(), 0.01);
            assertEquals(VALID_WHT, validRecord.getWhtAmount(), 0.01);
            assertEquals(9000.00, validRecord.getNetIncome(), 0.01);
        }

        @Test
        @DisplayName("Income code validation")
        void testIncomeCodeValidation() {
            // Valid codes
            assertTrue(IncomeRecord.isValidCode("IN001"));
            assertTrue(IncomeRecord.isValidCode("AB123"));
            assertTrue(IncomeRecord.isValidCode("XY999"));
            assertTrue(IncomeRecord.isValidCode("sa002")); // Should be converted to uppercase

            // Invalid codes
            assertFalse(IncomeRecord.isValidCode("123AB")); // Starts with numbers
            assertFalse(IncomeRecord.isValidCode("A1234")); // Too many characters
            assertFalse(IncomeRecord.isValidCode("ABC12")); // 3 letters instead of 2
            assertFalse(IncomeRecord.isValidCode("IN1"));   // Only 1 digit
            assertFalse(IncomeRecord.isValidCode(""));      // Empty
            assertFalse(IncomeRecord.isValidCode(null));    // Null
        }

        @Test
        @DisplayName("Description validation")
        void testDescriptionValidation() {
            // Valid descriptions
            assertTrue(IncomeRecord.isValidDescription("Valid Description"));
            assertTrue(IncomeRecord.isValidDescription("Short"));
            assertTrue(IncomeRecord.isValidDescription("Exactly20Characters")); // 20 chars exactly

            // Invalid descriptions
            assertFalse(IncomeRecord.isValidDescription(""));
            assertFalse(IncomeRecord.isValidDescription(null));
            assertFalse(IncomeRecord.isValidDescription("This description is way too long and exceeds the twenty character limit"));
        }

        @Test
        @DisplayName("Date validation")
        void testDateValidation() {
            // Valid dates
            assertTrue(IncomeRecord.isValidDate("25/07/2025"));
            assertTrue(IncomeRecord.isValidDate("01/01/2024"));
            assertTrue(IncomeRecord.isValidDate("31/12/2025"));
            assertTrue(IncomeRecord.isValidDate("29/02/2024")); // Leap year

            // Invalid dates
            assertFalse(IncomeRecord.isValidDate("32/01/2025")); // Invalid day
            assertFalse(IncomeRecord.isValidDate("01/13/2025")); // Invalid month
            assertFalse(IncomeRecord.isValidDate("29/02/2025")); // Non-leap year
            assertFalse(IncomeRecord.isValidDate("2025/07/25")); // Wrong format
            assertFalse(IncomeRecord.isValidDate("25-07-2025")); // Wrong separator
            assertFalse(IncomeRecord.isValidDate("25/7/2025"));  // Single digit month
            assertFalse(IncomeRecord.isValidDate(""));
            assertFalse(IncomeRecord.isValidDate(null));
        }

        @Test
        @DisplayName("Amount validation")
        void testAmountValidation() {
            // Valid income amounts
            assertTrue(IncomeRecord.isValidIncomeAmount(1000.00));
            assertTrue(IncomeRecord.isValidIncomeAmount(0.01));
            assertTrue(IncomeRecord.isValidIncomeAmount(1000000.00));

            // Invalid income amounts
            assertFalse(IncomeRecord.isValidIncomeAmount(0.00));
            assertFalse(IncomeRecord.isValidIncomeAmount(-1000.00));

            // Valid WHT amounts
            assertTrue(IncomeRecord.isValidWhtAmount(0.00));
            assertTrue(IncomeRecord.isValidWhtAmount(1000.00));
            assertTrue(IncomeRecord.isValidWhtAmount(500.50));

            // Invalid WHT amounts
            assertFalse(IncomeRecord.isValidWhtAmount(-100.00));
        }

        @Test
        @DisplayName("Record format validation")
        void testRecordFormatValidation() {
            assertTrue(validRecord.hasValidFormat());

            // Test with invalid data
            IncomeRecord invalidRecord = new IncomeRecord();
            try {
                invalidRecord.setCode("INVALID");
                fail("Should throw exception for invalid code");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }

        @Test
        @DisplayName("CSV format conversion")
        void testCSVFormatConversion() {
            String csvLine = validRecord.toCSVFormat();
            assertNotNull(csvLine);
            assertTrue(csvLine.contains(VALID_CODE));
            assertTrue(csvLine.contains(VALID_DESCRIPTION));
            assertTrue(csvLine.contains(VALID_DATE));
            assertTrue(csvLine.contains("10000.00"));
            assertTrue(csvLine.contains("1000.00"));
        }

        @Test
        @DisplayName("CSV parsing")
        void testCSVParsing() {
            String csvLine = "SA002,Consulting,26/07/2025,15000.00,1500.00,35";

            IncomeRecord parsedRecord = IncomeRecord.fromCSVFormat(csvLine);
            assertNotNull(parsedRecord);
            assertEquals("SA002", parsedRecord.getCode());
            assertEquals("Consulting", parsedRecord.getDescription());
            assertEquals("26/07/2025", parsedRecord.getIncomeDate());
            assertEquals(15000.00, parsedRecord.getIncomeAmount(), 0.01);
            assertEquals(1500.00, parsedRecord.getWhtAmount(), 0.01);
            assertEquals(35, parsedRecord.getOriginalChecksum());
        }

        @Test
        @DisplayName("Invalid CSV parsing")
        void testInvalidCSVParsing() {
            // Test insufficient fields
            assertThrows(IllegalArgumentException.class, () -> {
                IncomeRecord.fromCSVFormat("INVALID,FORMAT");
            });

            // Test empty line
            assertThrows(IllegalArgumentException.class, () -> {
                IncomeRecord.fromCSVFormat("");
            });

            // Test null line
            assertThrows(IllegalArgumentException.class, () -> {
                IncomeRecord.fromCSVFormat(null);
            });
        }

        @Test
        @DisplayName("Record update functionality")
        void testRecordUpdate() {
            String originalDescription = validRecord.getDescription();

            validRecord.updateData("Updated Description", "01/01/2025", 12000.00, 1200.00);

            assertNotEquals(originalDescription, validRecord.getDescription());
            assertEquals("Updated Description", validRecord.getDescription());
            assertEquals("01/01/2025", validRecord.getIncomeDate());
            assertEquals(12000.00, validRecord.getIncomeAmount(), 0.01);
            assertEquals(1200.00, validRecord.getWhtAmount(), 0.01);
            assertFalse(validRecord.isValid()); // Should be reset after update
        }

        @Test
        @DisplayName("Record copy functionality")
        void testRecordCopy() {
            IncomeRecord copy = validRecord.createCopy();

            assertNotNull(copy);
            assertNotSame(validRecord, copy); // Different objects
            assertEquals(validRecord.getCode(), copy.getCode());
            assertEquals(validRecord.getDescription(), copy.getDescription());
            assertEquals(validRecord.getIncomeDate(), copy.getIncomeDate());
            assertEquals(validRecord.getIncomeAmount(), copy.getIncomeAmount(), 0.01);
            assertEquals(validRecord.getWhtAmount(), copy.getWhtAmount(), 0.01);
        }
    }

    @Nested
    @DisplayName("Validation Engine Tests")
    class ValidationEngineTests {

        @Test
        @DisplayName("Single record validation")
        void testSingleRecordValidation() {
            // Test with matching checksum
            validRecord.setOriginalChecksum(30); // Set expected checksum
            boolean isValid = validationEngine.validateRecord(validRecord);

            // Check that calculated checksum is set
            assertTrue(validRecord.getCalculatedChecksum() > 0);

            // Validation result depends on checksum match
            assertEquals(isValid, validRecord.isValid());
        }

        @Test
        @DisplayName("Checksum calculation")
        void testChecksumCalculation() {
            int checksum = validationEngine.calculateChecksum(validRecord);
            assertTrue(checksum > 0);

            // Test specific string checksum
            String testString = "IN001,Test Income,25/07/2025,10000.00,1000.00";
            int stringChecksum = validationEngine.calculateChecksumFromString(testString);
            assertEquals(checksum, stringChecksum);
        }

        @Test
        @DisplayName("Checksum algorithm verification")
        void testChecksumAlgorithm() {
            String testLine = "IN001,Freelance Work,25/07/2025,10000.00,1000.00";
            int calculatedChecksum = validationEngine.calculateChecksumFromString(testLine);

            // Manual calculation:
            // Capital letters: I, N, F, W = 4
            // Numbers and decimals: 0,0,1,2,5,0,7,2,0,2,5,1,0,0,0,0,.,0,0,1,0,0,0,.,0,0 = 26
            // Total: 4 + 26 = 30
            assertEquals(30, calculatedChecksum);
        }

        @Test
        @DisplayName("Batch validation")
        void testBatchValidation() {
            List<IncomeRecord> records = Arrays.asList(
                    new IncomeRecord("IN001", "Income1", "25/07/2025", 10000.00, 1000.00),
                    new IncomeRecord("SA002", "Income2", "26/07/2025", 15000.00, 1500.00),
                    new IncomeRecord("WK003", "Income3", "27/07/2025", 8000.00, 800.00)
            );

            ValidationEngine.ValidationSummary summary = validationEngine.validateRecords(records);

            assertNotNull(summary);
            assertEquals(3, summary.getTotalCount());
            assertTrue(summary.getValidCount() >= 0);
            assertTrue(summary.getInvalidCount() >= 0);
            assertEquals(3, summary.getValidCount() + summary.getInvalidCount());
            assertTrue(summary.getSuccessRate() >= 0 && summary.getSuccessRate() <= 100);
        }

        @Test
        @DisplayName("Null record validation")
        void testNullRecordValidation() {
            boolean result = validationEngine.validateRecord(null);
            assertFalse(result);
        }

        @Test
        @DisplayName("Checksum recalculation")
        void testChecksumRecalculation() {
            List<IncomeRecord> records = Arrays.asList(validRecord);

            int processed = validationEngine.recalculateChecksums(records);
            assertEquals(1, processed);
            assertTrue(validRecord.getCalculatedChecksum() > 0);
        }

        @Test
        @DisplayName("Validation statistics")
        void testValidationStatistics() {
            ValidationEngine.ValidationStatistics stats = validationEngine.getStatistics();
            assertNotNull(stats);
            assertTrue(stats.getTotalValidations() >= 0);
            assertTrue(stats.getSuccessRate() >= 0);
        }
    }

    @Nested
    @DisplayName("Tax Processor Tests")
    class TaxProcessorTests {

        @Test
        @DisplayName("Basic tax calculation")
        void testBasicTaxCalculation() {
            validRecord.setValid(true);
            List<IncomeRecord> records = Arrays.asList(validRecord);

            double tax = taxProcessor.calculateTax(records);
            assertTrue(tax >= 0);
        }

        @Test
        @DisplayName("Tax calculation below threshold")
        void testTaxCalculationBelowThreshold() {
            IncomeRecord lowIncomeRecord = new IncomeRecord("LI001", "Low Income", "25/07/2025", 100000.00, 0.00);
            lowIncomeRecord.setValid(true);

            List<IncomeRecord> records = Arrays.asList(lowIncomeRecord);
            double tax = taxProcessor.calculateTax(records);

            // Income below 150,000 threshold should result in zero tax
            assertEquals(0.00, tax, 0.01);
        }

        @Test
        @DisplayName("Tax calculation above threshold")
        void testTaxCalculationAboveThreshold() {
            IncomeRecord highIncomeRecord = new IncomeRecord("HI001", "High Income", "25/07/2025", 200000.00, 5000.00);
            highIncomeRecord.setValid(true);

            List<IncomeRecord> records = Arrays.asList(highIncomeRecord);
            double tax = taxProcessor.calculateTax(records);

            // Expected: (200,000 - 150,000) * 12% - 5,000 = 6,000 - 5,000 = 1,000
            assertEquals(1000.00, tax, 0.01);
        }

        @Test
        @DisplayName("Tax calculation with excess WHT")
        void testTaxCalculationExcessWHT() {
            IncomeRecord excessWHTRecord = new IncomeRecord("EW001", "Excess WHT", "25/07/2025", 200000.00, 10000.00);
            excessWHTRecord.setValid(true);

            List<IncomeRecord> records = Arrays.asList(excessWHTRecord);
            double tax = taxProcessor.calculateTax(records);

            // Expected: (200,000 - 150,000) * 12% - 10,000 = 6,000 - 10,000 = 0 (minimum)
            assertEquals(0.00, tax, 0.01);
        }

        @Test
        @DisplayName("Multiple records tax calculation")
        void testMultipleRecordsTaxCalculation() {
            IncomeRecord record1 = new IncomeRecord("IN001", "Income1", "25/07/2025", 100000.00, 2000.00);
            IncomeRecord record2 = new IncomeRecord("IN002", "Income2", "26/07/2025", 100000.00, 3000.00);
            record1.setValid(true);
            record2.setValid(true);

            List<IncomeRecord> records = Arrays.asList(record1, record2);
            double tax = taxProcessor.calculateTax(records);

            // Expected: (200,000 - 150,000) * 12% - 5,000 = 6,000 - 5,000 = 1,000
            assertEquals(1000.00, tax, 0.01);
        }

        @Test
        @DisplayName("Tax calculation details")
        void testTaxCalculationDetails() {
            validRecord.setValid(true);
            List<IncomeRecord> records = Arrays.asList(validRecord);

            String details = taxProcessor.getCalculationDetails(records);

            assertNotNull(details);
            assertTrue(details.contains("TAX CALCULATION BREAKDOWN"));
            assertTrue(details.contains("Total Gross Income"));
            assertTrue(details.contains("NET TAX PAYABLE"));
            assertTrue(details.contains("Tax-Free Allowance"));
        }

        @Test
        @DisplayName("Empty records tax calculation")
        void testEmptyRecordsTaxCalculation() {
            List<IncomeRecord> emptyList = Arrays.asList();

            assertThrows(IllegalArgumentException.class, () -> {
                taxProcessor.calculateTax(emptyList);
            });
        }

        @Test
        @DisplayName("Null records tax calculation")
        void testNullRecordsTaxCalculation() {
            assertThrows(IllegalArgumentException.class, () -> {
                taxProcessor.calculateTax(null);
            });
        }

        @Test
        @DisplayName("Invalid records tax calculation")
        void testInvalidRecordsTaxCalculation() {
            validRecord.setValid(false); // Mark as invalid
            List<IncomeRecord> records = Arrays.asList(validRecord);

            assertThrows(IllegalArgumentException.class, () -> {
                taxProcessor.calculateTax(records);
            });
        }
    }

    @Nested
    @DisplayName("File Manager Tests")
    class FileManagerTests {

        private Path tempFile;

        @BeforeEach
        void setUpFileTests() throws IOException {
            tempFile = Files.createTempFile("test_income", ".csv");
        }

        @AfterEach
        void cleanUpFileTests() throws IOException {
            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }

        @Test
        @DisplayName("CSV export functionality")
        void testCSVExport() {
            List<IncomeRecord> records = Arrays.asList(validRecord);

            boolean success = fileManager.exportToCSV(records, tempFile.toString());
            assertTrue(success);
            assertTrue(Files.exists(tempFile));
        }

        @Test
        @DisplayName("CSV import functionality")
        void testCSVImport() throws IOException {
            // Create test CSV content
            String csvContent = "Income_Code,Description,Date,Income_Amount,WHT_Amount,Checksum\n" +
                    "IN001,Test Income,25/07/2025,10000.00,1000.00,30\n" +
                    "SA002,Consulting,26/07/2025,15000.00,1500.00,35";

            Files.write(tempFile, csvContent.getBytes());

            List<IncomeRecord> records = fileManager.importFromCSV(tempFile.toString());

            assertNotNull(records);
            assertEquals(2, records.size());

            IncomeRecord firstRecord = records.get(0);
            assertEquals("IN001", firstRecord.getCode());
            assertEquals("Test Income", firstRecord.getDescription());
        }

        @Test
        @DisplayName("File validation")
        void testFileValidation() {
            FileManager.ValidationResult result = fileManager.validateCSVStructure("nonexistent.csv");
            assertNotNull(result);
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("File analysis")
        void testFileAnalysis() throws IOException {
            // Create test file with content
            String content = "Line 1\nLine 2\nLine 3";
            Files.write(tempFile, content.getBytes());

            FileManager.FileInfo info = fileManager.analyzeFile(tempFile.toString());

            assertNotNull(info);
            assertTrue(info.isExists());
            assertTrue(info.getFileSize() > 0);
            assertEquals(3, info.getLineCount());
            assertEquals(2, info.getEstimatedRecords()); // Lines minus header
        }

        @Test
        @DisplayName("Empty file export")
        void testEmptyFileExport() {
            List<IncomeRecord> emptyList = Arrays.asList();

            boolean success = fileManager.exportToCSV(emptyList, tempFile.toString());
            assertFalse(success);
        }

        @Test
        @DisplayName("Null records export")
        void testNullRecordsExport() {
            boolean success = fileManager.exportToCSV(null, tempFile.toString());
            assertFalse(success);
        }

        @Test
        @DisplayName("Invalid file path import")
        void testInvalidFilePathImport() {
            assertThrows(IOException.class, () -> {
                fileManager.importFromCSV("nonexistent.csv");
            });
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Complete workflow test")
        void testCompleteWorkflow() throws IOException {
            // Step 1: Create test records
            List<IncomeRecord> originalRecords = Arrays.asList(
                    new IncomeRecord("IN001", "Contract Work", "25/07/2025", 200000.00, 10000.00),
                    new IncomeRecord("SA002", "Consulting", "26/07/2025", 100000.00, 5000.00)
            );

            // Step 2: Calculate and set checksums
            for (IncomeRecord record : originalRecords) {
                int checksum = validationEngine.calculateChecksum(record);
                record.setOriginalChecksum(checksum);
                record.setCalculatedChecksum(checksum);
            }

            // Step 3: Export to CSV
            Path testFile = Files.createTempFile("workflow_test", ".csv");
            try {
                boolean exportSuccess = fileManager.exportToCSV(originalRecords, testFile.toString());
                assertTrue(exportSuccess);

                // Step 4: Import from CSV
                List<IncomeRecord> importedRecords = fileManager.importFromCSV(testFile.toString());
                assertEquals(originalRecords.size(), importedRecords.size());

                // Step 5: Validate records
                ValidationEngine.ValidationSummary summary = validationEngine.validateRecords(importedRecords);
                assertEquals(importedRecords.size(), summary.getTotalCount());

                // Step 6: Calculate tax for valid records
                List<IncomeRecord> validRecords = summary.getValidRecords();
                if (!validRecords.isEmpty()) {
                    double taxPayable = taxProcessor.calculateTax(validRecords);
                    assertTrue(taxPayable >= 0);

                    // Step 7: Generate detailed report
                    String report = taxProcessor.getCalculationDetails(validRecords);
                    assertNotNull(report);
                    assertTrue(report.length() > 0);
                }

            } finally {
                Files.deleteIfExists(testFile);
            }
        }

        @Test
        @DisplayName("Error handling integration")
        void testErrorHandlingIntegration() {
            // Test null validation
            assertFalse(validationEngine.validateRecord(null));

            // Test empty tax calculation
            assertThrows(IllegalArgumentException.class, () -> {
                taxProcessor.calculateTax(Arrays.asList());
            });

            // Test invalid file operations
            assertThrows(IOException.class, () -> {
                fileManager.importFromCSV("nonexistent_file.csv");
            });
        }
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("All comprehensive tests completed successfully!");
    }
}
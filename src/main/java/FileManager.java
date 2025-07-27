/**
 * File Manager - Alternative Implementation
 * Handles all file operations for income data processing
 */

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FileManager {

    // Constants
    private static final String CSV_HEADER = "Income_Code,Description,Date,Income_Amount,WHT_Amount,Checksum";
    private static final String ENCODING = "UTF-8";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Constructor
    public FileManager() {
        // Initialize file manager
    }

    /**
     * Import income records from CSV file
     *
     * @param filePath Path to CSV file
     * @return List of imported IncomeRecord objects
     * @throws IOException If file operation fails
     */
    public List<IncomeRecord> importFromCSV(String filePath) throws IOException {
        validateFilePath(filePath);

        List<IncomeRecord> records = new ArrayList<>();
        List<String> errorLog = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            int lineNumber = 0;
            boolean headerProcessed = false;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // Handle header
                if (!headerProcessed) {
                    if (isHeaderLine(line)) {
                        headerProcessed = true;
                        continue;
                    } else {
                        // No header found, treat first line as data
                        headerProcessed = true;
                    }
                }

                // Parse data line
                try {
                    IncomeRecord record = IncomeRecord.fromCSVFormat(line);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    String error = String.format("Line %d: %s - %s", lineNumber, e.getMessage(), line);
                    errorLog.add(error);
                    System.err.println("Parse error: " + error);
                }
            }
        }

        // Log import results
        System.out.println("Import completed:");
        System.out.println("  Records imported: " + records.size());
        System.out.println("  Errors encountered: " + errorLog.size());

        if (!errorLog.isEmpty()) {
            System.err.println("Import errors:");
            for (String error : errorLog) {
                System.err.println("  " + error);
            }
        }

        return records;
    }

    /**
     * Export income records to CSV file
     *
     * @param records List of records to export
     * @param filePath Target file path
     * @return true if successful, false otherwise
     */
    public boolean exportToCSV(List<IncomeRecord> records, String filePath) {
        if (records == null || records.isEmpty()) {
            System.err.println("No records to export");
            return false;
        }

        try {
            Path path = Paths.get(filePath);

            // Create directories if needed
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            // Create backup if file exists
            if (Files.exists(path)) {
                createFileBackup(filePath);
            }

            // Write CSV file
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                // Write header
                writer.write(CSV_HEADER);
                writer.newLine();

                // Write records
                for (IncomeRecord record : records) {
                    writer.write(record.toCSVFormat());
                    writer.newLine();
                }
            }

            System.out.println("Successfully exported " + records.size() + " records to: " + filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save records to data storage format
     *
     * @param records List of records to save
     * @param filePath Target file path
     * @return true if successful
     */
    public boolean saveToDataFormat(List<IncomeRecord> records, String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                for (IncomeRecord record : records) {
                    writer.write(record.toDataFormat());
                    writer.newLine();
                }
            }

            System.out.println("Data saved to: " + filePath);
            return true;

        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load records from data storage format
     *
     * @param filePath Source file path
     * @return List of loaded records
     * @throws IOException If file operation fails
     */
    public List<IncomeRecord> loadFromDataFormat(String filePath) throws IOException {
        List<IncomeRecord> records = new ArrayList<>();

        if (!Files.exists(Paths.get(filePath))) {
            System.out.println("Data file not found: " + filePath);
            return records;
        }

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                try {
                    IncomeRecord record = IncomeRecord.fromDataFormat(line);
                    records.add(record);
                } catch (Exception e) {
                    System.err.println("Error loading line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Loaded " + records.size() + " records from: " + filePath);
        return records;
    }

    /**
     * Validate file path and accessibility
     *
     * @param filePath File path to validate
     * @throws IOException If file is not accessible
     */
    private void validateFilePath(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IOException("File path cannot be empty");
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("File is not readable: " + filePath);
        }

        if (!filePath.toLowerCase().endsWith(".csv")) {
            throw new IOException("File must be a CSV file: " + filePath);
        }
    }

    /**
     * Check if a line is a CSV header
     *
     * @param line Line to check
     * @return true if it's a header line
     */
    private boolean isHeaderLine(String line) {
        if (line == null) {
            return false;
        }

        String lowerLine = line.toLowerCase();
        return lowerLine.contains("income_code") ||
                lowerLine.contains("description") ||
                lowerLine.contains("checksum");
    }

    /**
     * Create backup of existing file
     *
     * @param filePath Original file path
     * @return Backup file path or null if failed
     */
    public String createFileBackup(String filePath) {
        try {
            Path originalPath = Paths.get(filePath);

            if (!Files.exists(originalPath)) {
                return null;
            }

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String backupPath = filePath.replace(".csv", "_backup_" + timestamp + ".csv");

            Files.copy(originalPath, Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Backup created: " + backupPath);
            return backupPath;

        } catch (IOException e) {
            System.err.println("Backup creation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get file information and statistics
     *
     * @param filePath File to analyze
     * @return FileInfo object with details
     */
    public FileInfo analyzeFile(String filePath) {
        FileInfo info = new FileInfo();
        info.setFilePath(filePath);

        try {
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                info.setExists(true);
                info.setFileSize(Files.size(path));
                info.setLastModified(Files.getLastModifiedTime(path).toMillis());
                info.setReadable(Files.isReadable(path));
                info.setWritable(Files.isWritable(path));

                // Count lines
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    long lineCount = reader.lines().count();
                    info.setLineCount(lineCount);
                }

                // Estimate record count (excluding header)
                info.setEstimatedRecords(Math.max(0, info.getLineCount() - 1));

            } else {
                info.setExists(false);
            }

        } catch (IOException e) {
            info.setError("Error analyzing file: " + e.getMessage());
        }

        return info;
    }

    /**
     * Validate CSV file structure
     *
     * @param filePath CSV file to validate
     * @return ValidationResult with details
     */
    public ValidationResult validateCSVStructure(String filePath) {
        ValidationResult result = new ValidationResult();

        try {
            if (!filePath.toLowerCase().endsWith(".csv")) {
                result.addError("File must have .csv extension");
                return result;
            }

            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                result.addError("File does not exist");
                return result;
            }

            if (Files.size(path) == 0) {
                result.addError("File is empty");
                return result;
            }

            // Check first few lines
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    result.addError("File appears to be empty");
                    return result;
                }

                // Check if first line looks like CSV
                if (!firstLine.contains(",")) {
                    result.addWarning("File does not appear to be comma-separated");
                }

                // Check header
                if (isHeaderLine(firstLine)) {
                    result.addInfo("Valid CSV header detected");
                } else {
                    result.addWarning("No standard header found");
                }

                result.setValid(true);
            }

        } catch (IOException e) {
            result.addError("Error validating file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Clean up temporary and backup files
     *
     * @param directory Directory to clean
     * @param olderThanDays Remove files older than specified days
     * @return Number of files cleaned
     */
    public int cleanupFiles(String directory, int olderThanDays) {
        int cleanedCount = 0;

        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                return 0;
            }

            long cutoffTime = System.currentTimeMillis() - (olderThanDays * 24L * 60L * 60L * 1000L);

            Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("backup"))
                    .forEach(path -> {
                        try {
                            if (Files.getLastModifiedTime(path).toMillis() < cutoffTime) {
                                Files.delete(path);
                                System.out.println("Cleaned up: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to clean: " + path.getFileName());
                        }
                    });

        } catch (IOException e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }

        return cleanedCount;
    }

    // Helper Classes
    public static class FileInfo {
        private String filePath;
        private boolean exists;
        private long fileSize;
        private long lastModified;
        private boolean readable;
        private boolean writable;
        private long lineCount;
        private long estimatedRecords;
        private String error;

        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public boolean isExists() { return exists; }
        public void setExists(boolean exists) { this.exists = exists; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public boolean isReadable() { return readable; }
        public void setReadable(boolean readable) { this.readable = readable; }

        public boolean isWritable() { return writable; }
        public void setWritable(boolean writable) { this.writable = writable; }

        public long getLineCount() { return lineCount; }
        public void setLineCount(long lineCount) { this.lineCount = lineCount; }

        public long getEstimatedRecords() { return estimatedRecords; }
        public void setEstimatedRecords(long estimatedRecords) { this.estimatedRecords = estimatedRecords; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        @Override
        public String toString() {
            return String.format("FileInfo{path='%s', exists=%b, size=%d, records=%d}",
                    filePath, exists, fileSize, estimatedRecords);
        }
    }

    public static class ValidationResult {
        private boolean valid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> info = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { errors.add(error); }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { warnings.add(warning); }

        public List<String> getInfo() { return info; }
        public void addInfo(String info) { this.info.add(info); }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
}
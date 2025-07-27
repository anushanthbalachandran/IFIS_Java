/**
 * Income File Import System - Alternative Implementation
 * Government Tax Department Digital Initiative
 */

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MainApplication extends Application {

    // Core Components
    private Stage mainStage;
    private FileManager fileManager;
    private ValidationEngine validationEngine;
    private TaxProcessor taxProcessor;

    // UI Components - Different Layout Approach
    private TabPane mainTabPane;
    private TextField fileLocationField;
    private TableView<IncomeRecord> dataTable;
    private ObservableList<IncomeRecord> recordData;
    private ProgressBar validationProgress;

    // Status Components
    private Label recordCountLabel;
    private Label validCountLabel;
    private Label invalidCountLabel;
    private Label taxAmountLabel;

    // Control Components
    private Button importButton;
    private Button validateButton;
    private Button calculateButton;
    private Button exportButton;
    private Button removeInvalidButton;

    // Data Collections
    private List<IncomeRecord> allRecords;
    private List<IncomeRecord> validRecords;
    private List<IncomeRecord> invalidRecords;

    // Formatting
    private NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "LK"));

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;

        initializeSystem();

        // Create the UI first
        BorderPane mainInterface = buildMainInterface();

        // Now setup UI state and event handlers
        setupUserInterface();
        configureEventHandlers();

        primaryStage.setTitle("Income File Import System - Tax Department Portal");
        primaryStage.setScene(new Scene(mainInterface, 1300, 900));
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(500);
        primaryStage.setMaxHeight(700);
        primaryStage.show();

        displayMessage("System initialized. Ready for file import operations.");
    }

    private void initializeSystem() {
        fileManager = new FileManager();
        validationEngine = new ValidationEngine();
        taxProcessor = new TaxProcessor();

        recordData = FXCollections.observableArrayList();
        allRecords = new ArrayList<>();
        validRecords = new ArrayList<>();
        invalidRecords = new ArrayList<>();
    }

    private BorderPane buildMainInterface() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Top: Header and File Input
        mainLayout.setTop(createHeaderSection());

        // Center: Tabbed Interface
        mainLayout.setCenter(createTabSection());

        // Bottom: Status Bar
        mainLayout.setBottom(createStatusSection());

        return mainLayout;
    }

    private VBox createHeaderSection() {
        VBox headerBox = new VBox(10);
        headerBox.setPadding(new Insets(10));
        headerBox.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-background-radius: 10;");

        // Title
        Label titleLabel = new Label("Income File Import System");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subtitleLabel = new Label("Government Tax Department - Digital Processing Portal");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e0e0e0;");

        // File Selection Area
        HBox fileSelectionBox = createFileSelectionArea();

        headerBox.getChildren().addAll(titleLabel, subtitleLabel, fileSelectionBox);
        headerBox.setAlignment(Pos.CENTER);

        return headerBox;
    }

    private HBox createFileSelectionArea() {
        HBox fileBox = new HBox(10);
        fileBox.setPadding(new Insets(15));
        fileBox.setAlignment(Pos.CENTER);
        fileBox.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");

        Label fileLabel = new Label("Select Income File:");
        fileLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        fileLocationField = new TextField();
        fileLocationField.setPromptText("Browse for CSV income file...");
        fileLocationField.setPrefWidth(400);
        fileLocationField.setStyle("-fx-font-size: 12px;");

        Button browseButton = new Button("Browse Files");
        browseButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        browseButton.setOnAction(e -> selectIncomeFile());

        // Initialize the class field button
        importButton = new Button("Import Data");
        importButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        importButton.setOnAction(e -> importIncomeData());

        fileBox.getChildren().addAll(fileLabel, fileLocationField, browseButton, importButton);
        HBox.setHgrow(fileLocationField, Priority.ALWAYS);

        return fileBox;
    }

    private TabPane createTabSection() {
        mainTabPane = new TabPane();
        mainTabPane.setStyle("-fx-background-color: #f5f5f5;");

        // Data View Tab
        Tab dataTab = new Tab("Income Records");
        dataTab.setContent(createDataViewSection());
        dataTab.setClosable(false);

        // Validation Tab
        Tab validationTab = new Tab("Validation & Processing");
        validationTab.setContent(createValidationSection());
        validationTab.setClosable(false);

        // Tax Calculation Tab
        Tab taxTab = new Tab("Tax Calculation");
        taxTab.setContent(createTaxSection());
        taxTab.setClosable(false);

        mainTabPane.getTabs().addAll(dataTab, validationTab, taxTab);

        return mainTabPane;
    }

    private VBox createDataViewSection() {
        VBox dataSection = new VBox(15);
        dataSection.setPadding(new Insets(20));

        // Table Controls
        HBox tableControls = new HBox(10);
        tableControls.setAlignment(Pos.CENTER_LEFT);

        Label tableTitle = new Label("Imported Income Records");
        tableTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button refreshButton = new Button("Refresh View");
        refreshButton.setOnAction(e -> refreshTableDisplay());

        Button editModeButton = new Button("Enable Editing");
        editModeButton.setOnAction(e -> toggleEditMode());

        tableControls.getChildren().addAll(tableTitle, refreshButton, editModeButton);
        HBox.setHgrow(tableTitle, Priority.ALWAYS);

        // Data Table
        dataTable = buildDataTable();

        dataSection.getChildren().addAll(tableControls, dataTable);
        VBox.setVgrow(dataTable, Priority.ALWAYS);

        return dataSection;
    }

    private TableView<IncomeRecord> buildDataTable() {
        TableView<IncomeRecord> table = new TableView<>();
        table.setItems(recordData);
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Income Code Column
        TableColumn<IncomeRecord, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCode()));
        codeCol.setPrefWidth(100);

        // Description Column
        TableColumn<IncomeRecord, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(200);

        // Date Column
        TableColumn<IncomeRecord, String> dateCol = new TableColumn<>("Income Date");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getIncomeDate()));
        dateCol.setPrefWidth(120);

        // Income Amount Column
        TableColumn<IncomeRecord, Double> incomeCol = new TableColumn<>("Income Amount");
        incomeCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getIncomeAmount()).asObject());
        incomeCol.setCellFactory(col -> new TableCell<IncomeRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs %.2f", item));
                }
            }
        });
        incomeCol.setPrefWidth(150);

        // WHT Amount Column
        TableColumn<IncomeRecord, Double> whtCol = new TableColumn<>("WHT Amount");
        whtCol.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getWhtAmount()).asObject());
        whtCol.setCellFactory(col -> new TableCell<IncomeRecord, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("Rs %.2f", item));
                }
            }
        });
        whtCol.setPrefWidth(150);

        // Checksum Columns
        TableColumn<IncomeRecord, Integer> originalChecksumCol = new TableColumn<>("Original CS");
        originalChecksumCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getOriginalChecksum()).asObject());
        originalChecksumCol.setPrefWidth(100);

        TableColumn<IncomeRecord, Integer> calculatedChecksumCol = new TableColumn<>("Calculated CS");
        calculatedChecksumCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCalculatedChecksum()).asObject());
        calculatedChecksumCol.setPrefWidth(110);

        // Status Column
        TableColumn<IncomeRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isValid() ? "Valid" : "Invalid"));
        statusCol.setCellFactory(col -> new TableCell<IncomeRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Valid".equals(item)) {
                        setStyle("-fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32;");
                    } else {
                        setStyle("-fx-background-color: #ffcdd2; -fx-text-fill: #c62828;");
                    }
                }
            }
        });
        statusCol.setPrefWidth(80);

        table.getColumns().addAll(codeCol, descCol, dateCol, incomeCol, whtCol,
                originalChecksumCol, calculatedChecksumCol, statusCol);

        return table;
    }

    private VBox createValidationSection() {
        VBox validationSection = new VBox(20);
        validationSection.setPadding(new Insets(20));

        // Validation Controls
        HBox validationControls = new HBox(15);
        validationControls.setAlignment(Pos.CENTER);
        validationControls.setPadding(new Insets(15));
        validationControls.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10;");

        // Initialize class field buttons
        validateButton = new Button("Run Validation");
        validateButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        validateButton.setPrefWidth(150);
        validateButton.setOnAction(e -> performValidation());

        removeInvalidButton = new Button("Remove Invalid Records");
        removeInvalidButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
        removeInvalidButton.setPrefWidth(180);
        removeInvalidButton.setOnAction(e -> removeInvalidRecords());
        removeInvalidButton.setDisable(true);

        Button recalculateButton = new Button("Recalculate Checksums");
        recalculateButton.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-size: 14px;");
        recalculateButton.setPrefWidth(180);
        recalculateButton.setOnAction(e -> recalculateAllChecksums());

        validationControls.getChildren().addAll(validateButton, removeInvalidButton, recalculateButton);

        // Progress Section
        VBox progressSection = new VBox(10);
        progressSection.setPadding(new Insets(15));
        progressSection.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 8;");

        Label progressLabel = new Label("Validation Progress");
        progressLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        validationProgress = new ProgressBar(0);
        validationProgress.setPrefWidth(400);
        validationProgress.setStyle("-fx-accent: #4CAF50;");

        progressSection.getChildren().addAll(progressLabel, validationProgress);
        progressSection.setAlignment(Pos.CENTER);

        // Statistics Grid
        GridPane statsGrid = createStatisticsGrid();

        validationSection.getChildren().addAll(validationControls, progressSection, statsGrid);

        return validationSection;
    }

    private GridPane createStatisticsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));
        grid.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");

        // Statistics Labels
        Label statsTitle = new Label("Validation Statistics");
        statsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Initialize the class field labels
        recordCountLabel = new Label("Total Records: 0");
        recordCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        validCountLabel = new Label("Valid Records: 0");
        validCountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");

        invalidCountLabel = new Label("Invalid Records: 0");
        invalidCountLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f44336; -fx-font-weight: bold;");

        // Add to grid
        grid.add(statsTitle, 0, 0, 3, 1);
        grid.add(recordCountLabel, 0, 1);
        grid.add(validCountLabel, 1, 1);
        grid.add(invalidCountLabel, 2, 1);

        return grid;
    }

    private VBox createTaxSection() {
        VBox taxSection = new VBox(20);
        taxSection.setPadding(new Insets(20));

        // Tax Calculation Controls
        HBox taxControls = new HBox(15);
        taxControls.setAlignment(Pos.CENTER);
        taxControls.setPadding(new Insets(15));
        taxControls.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 10;");

        calculateButton = new Button("Calculate Tax Liability");
        calculateButton.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        calculateButton.setPrefWidth(200);
        calculateButton.setOnAction(e -> calculateTaxLiability());
        calculateButton.setDisable(true);

        Button showDetailsButton = new Button("Show Calculation Details");
        showDetailsButton.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-font-size: 14px;");
        showDetailsButton.setPrefWidth(200);
        showDetailsButton.setOnAction(e -> showTaxDetails());

        taxControls.getChildren().addAll(calculateButton, showDetailsButton);

        // Tax Summary
        VBox taxSummary = createTaxSummarySection();

        // Tax Formula Information
        VBox formulaInfo = createTaxFormulaSection();

        taxSection.getChildren().addAll(taxControls, taxSummary, formulaInfo);

        return taxSection;
    }

    private VBox createTaxSummarySection() {
        VBox summaryBox = new VBox(15);
        summaryBox.setPadding(new Insets(20));
        summaryBox.setStyle("-fx-background-color: #e8f5e8; -fx-border-color: #4CAF50; -fx-border-width: 2; -fx-border-radius: 10;");

        Label summaryTitle = new Label("Tax Calculation Summary");
        summaryTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        // Initialize the class field label
        taxAmountLabel = new Label("Tax Payable: Rs 0.00");
        taxAmountLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

        summaryBox.getChildren().addAll(summaryTitle, taxAmountLabel);
        summaryBox.setAlignment(Pos.CENTER);

        return summaryBox;
    }

    private VBox createTaxFormulaSection() {
        VBox formulaBox = new VBox(10);
        formulaBox.setPadding(new Insets(15));
        formulaBox.setStyle("-fx-background-color: #fff; -fx-border-color: #ddd; -fx-border-radius: 8;");

        Label formulaTitle = new Label("Tax Calculation Formula");
        formulaTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label formulaText = new Label("Tax Payable = [(Total Income - Rs 150,000) × 12%] - Total WHT Paid");
        formulaText.setStyle("-fx-font-size: 14px; -fx-font-family: monospace; -fx-background-color: #f5f5f5; -fx-padding: 10;");

        Label noteText = new Label("Note: Tax-free threshold is Rs 150,000 as per Sri Lankan tax regulations");
        noteText.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-style: italic;");

        formulaBox.getChildren().addAll(formulaTitle, formulaText, noteText);

        return formulaBox;
    }

    private HBox createStatusSection() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #37474f; -fx-text-fill: white;");

        Label statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        exportButton = new Button("Export Results");
        exportButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        exportButton.setOnAction(e -> exportResults());
        exportButton.setDisable(true);

        statusBar.getChildren().addAll(statusLabel, exportButton);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        return statusBar;
    }

    private void setupUserInterface() {
        // Initialize UI state after components are created
        updateCounters();

        // Set initial button states
        validateButton.setDisable(true);
        calculateButton.setDisable(true);
        exportButton.setDisable(true);
        removeInvalidButton.setDisable(true);
    }

    private void configureEventHandlers() {
        // Table selection handler
        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayMessage("Selected: " + newVal.getCode() + " - " + newVal.getDescription());
            }
        });
    }

    private void selectIncomeFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Income CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File selectedFile = fileChooser.showOpenDialog(mainStage);
        if (selectedFile != null) {
            fileLocationField.setText(selectedFile.getAbsolutePath());
            displayMessage("Selected file: " + selectedFile.getName());
        }
    }

    private void importIncomeData() {
        String filePath = fileLocationField.getText().trim();
        if (filePath.isEmpty()) {
            showAlert("No File Selected", "Please select a CSV file to import.", Alert.AlertType.WARNING);
            return;
        }

        try {
            displayMessage("Importing data from: " + filePath);

            List<IncomeRecord> importedRecords = fileManager.importFromCSV(filePath);

            if (importedRecords != null && !importedRecords.isEmpty()) {
                allRecords.clear();
                allRecords.addAll(importedRecords);

                recordData.clear();
                recordData.addAll(importedRecords);

                updateCounters();
                displayMessage("Successfully imported " + importedRecords.size() + " records");

                // Enable validation
                validateButton.setDisable(false);

                // Auto-switch to validation tab
                mainTabPane.getSelectionModel().select(1);

            } else {
                showAlert("Import Failed", "No valid records found in the selected file.", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            showAlert("Import Error", "Failed to import file: " + e.getMessage(), Alert.AlertType.ERROR);
            displayMessage("Import failed: " + e.getMessage());
        }
    }

    private void performValidation() {
        if (allRecords.isEmpty()) {
            showAlert("No Data", "No records to validate. Please import data first.", Alert.AlertType.WARNING);
            return;
        }

        displayMessage("Running validation on " + allRecords.size() + " records...");

        // Reset progress
        validationProgress.setProgress(0);

        validRecords.clear();
        invalidRecords.clear();

        for (int i = 0; i < allRecords.size(); i++) {
            IncomeRecord record = allRecords.get(i);

            // Perform validation
            boolean isValid = validationEngine.validateRecord(record);
            record.setValid(isValid);

            if (isValid) {
                validRecords.add(record);
            } else {
                invalidRecords.add(record);
            }

            // Update progress
            validationProgress.setProgress((double) (i + 1) / allRecords.size());
        }

        // Refresh table
        dataTable.refresh();
        updateCounters();

        // Enable actions
        removeInvalidButton.setDisable(invalidRecords.isEmpty());
        calculateButton.setDisable(validRecords.isEmpty());
        exportButton.setDisable(false);

        displayMessage("Validation complete: " + validRecords.size() + " valid, " + invalidRecords.size() + " invalid");

        // Switch to tax calculation tab if we have valid records
        if (!validRecords.isEmpty()) {
            mainTabPane.getSelectionModel().select(2);
        }
    }

    private void calculateTaxLiability() {
        if (validRecords.isEmpty()) {
            showAlert("No Valid Records", "No valid records available for tax calculation.", Alert.AlertType.WARNING);
            return;
        }

        try {
            double taxPayable = taxProcessor.calculateTax(validRecords);

            taxAmountLabel.setText(String.format("Tax Payable: Rs %.2f", taxPayable));

            displayMessage("Tax calculated: Rs " + String.format("%.2f", taxPayable) +
                    " for " + validRecords.size() + " valid records");

        } catch (Exception e) {
            showAlert("Calculation Error", "Failed to calculate tax: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void removeInvalidRecords() {
        if (invalidRecords.isEmpty()) {
            showAlert("No Invalid Records", "No invalid records to remove.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Removal");
        confirmAlert.setHeaderText("Remove Invalid Records");
        confirmAlert.setContentText("Are you sure you want to remove " + invalidRecords.size() + " invalid records?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            allRecords.removeAll(invalidRecords);
            recordData.removeAll(invalidRecords);

            displayMessage("Removed " + invalidRecords.size() + " invalid records");
            invalidRecords.clear();

            updateCounters();
            removeInvalidButton.setDisable(true);
        }
    }

    private void recalculateAllChecksums() {
        if (allRecords.isEmpty()) {
            showAlert("No Data", "No records available for checksum recalculation.", Alert.AlertType.WARNING);
            return;
        }

        for (IncomeRecord record : allRecords) {
            int newChecksum = validationEngine.calculateChecksum(record);
            record.setCalculatedChecksum(newChecksum);
        }

        dataTable.refresh();
        displayMessage("Recalculated checksums for all " + allRecords.size() + " records");
    }

    private void refreshTableDisplay() {
        dataTable.refresh();
        updateCounters();
        displayMessage("Table view refreshed");
    }

    private void toggleEditMode() {
        boolean currentEditable = dataTable.isEditable();
        dataTable.setEditable(!currentEditable);
        displayMessage("Edit mode " + (currentEditable ? "disabled" : "enabled"));
    }

    private void showTaxDetails() {
        if (validRecords.isEmpty()) {
            showAlert("No Data", "No valid records for tax calculation details.", Alert.AlertType.INFORMATION);
            return;
        }

        String details = taxProcessor.getCalculationDetails(validRecords);

        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Tax Calculation Details");
        detailsAlert.setHeaderText("Detailed Tax Calculation Breakdown");
        detailsAlert.setContentText(details);
        detailsAlert.getDialogPane().setPrefWidth(500);
        detailsAlert.showAndWait();
    }

    private void exportResults() {
        if (allRecords.isEmpty()) {
            showAlert("No Data", "No data available for export.", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Results");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("processed_income_data.csv");

        File exportFile = fileChooser.showSaveDialog(mainStage);
        if (exportFile != null) {
            try {
                boolean success = fileManager.exportToCSV(allRecords, exportFile.getAbsolutePath());
                if (success) {
                    displayMessage("Data exported successfully to: " + exportFile.getName());
                    showAlert("Export Complete", "Data exported successfully to:\n" + exportFile.getAbsolutePath(),
                            Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Export Failed", "Failed to export data to file.", Alert.AlertType.ERROR);
                }
            } catch (Exception e) {
                showAlert("Export Error", "Error during export: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void updateCounters() {
        recordCountLabel.setText("Total Records: " + allRecords.size());
        validCountLabel.setText("Valid Records: " + validRecords.size());
        invalidCountLabel.setText("Invalid Records: " + invalidRecords.size());
    }

    private void displayMessage(String message) {
        System.out.println("[IFIS] " + message);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
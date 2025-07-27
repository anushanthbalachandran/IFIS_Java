/**
 * Tax Processor - Alternative Implementation
 * Advanced tax calculation engine with Sri Lankan tax regulations
 */

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TaxProcessor {

    // Tax Regulation Constants
    private static final double TAX_FREE_ALLOWANCE = 150000.00; // Rs 150,000
    private static final double STANDARD_TAX_RATE = 0.12; // 12%
    private static final double HIGH_INCOME_THRESHOLD = 5000000.00; // Rs 5 million
    private static final double HIGH_INCOME_RATE = 0.18; // 18% for high earners

    // Formatting and Display
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#0.00");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Calculation History
    private final List<TaxCalculationRecord> calculationHistory = new ArrayList<>();

    // Constructor
    public TaxProcessor() {
        // Initialize tax processor
    }

    /**
     * Calculate tax liability for list of valid income records
     *
     * @param validRecords List of validated income records
     * @return Total tax payable amount
     */
    public double calculateTax(List<IncomeRecord> validRecords) {
        if (validRecords == null || validRecords.isEmpty()) {
            throw new IllegalArgumentException("No valid records provided for tax calculation");
        }

        // Validate all records are actually valid
        List<IncomeRecord> actuallyValid = validRecords.stream()
                .filter(IncomeRecord::isValid)
                .collect(Collectors.toList());

        if (actuallyValid.isEmpty()) {
            throw new IllegalArgumentException("No valid records found in the provided list");
        }

        // Perform tax calculation
        TaxCalculationResult result = performDetailedTaxCalculation(actuallyValid);

        // Store in history
        TaxCalculationRecord record = new TaxCalculationRecord(
                LocalDateTime.now(),
                actuallyValid.size(),
                result
        );
        calculationHistory.add(record);

        // Log calculation summary
        logCalculationSummary(result);

        return result.getFinalTaxPayable();
    }

    /**
     * Perform detailed tax calculation with breakdown
     *
     * @param records Valid income records
     * @return Detailed calculation result
     */
    private TaxCalculationResult performDetailedTaxCalculation(List<IncomeRecord> records) {
        TaxCalculationResult result = new TaxCalculationResult();

        // Step 1: Calculate totals
        double totalIncome = calculateTotalIncome(records);
        double totalWHT = calculateTotalWHT(records);

        result.setTotalIncome(totalIncome);
        result.setTotalWHT(totalWHT);
        result.setRecordCount(records.size());

        // Step 2: Calculate taxable income
        double taxableIncome = Math.max(0, totalIncome - TAX_FREE_ALLOWANCE);
        result.setTaxableIncome(taxableIncome);
        result.setTaxFreeAllowance(TAX_FREE_ALLOWANCE);

        // Step 3: Calculate gross tax using progressive rates
        double grossTax = calculateProgressiveTax(taxableIncome);
        result.setGrossTax(grossTax);

        // Step 4: Calculate net tax after WHT
        double netTaxPayable = Math.max(0, grossTax - totalWHT);
        result.setFinalTaxPayable(netTaxPayable);

        // Step 5: Calculate additional metrics
        result.setEffectiveTaxRate(totalIncome > 0 ? (netTaxPayable / totalIncome) * 100 : 0);
        result.setWHTCoverage(grossTax > 0 ? (totalWHT / grossTax) * 100 : 0);
        result.setAverageIncomePerRecord(totalIncome / records.size());
        result.setTaxSavingsFromAllowance(TAX_FREE_ALLOWANCE * STANDARD_TAX_RATE);

        return result;
    }

    /**
     * Calculate progressive tax based on income brackets
     *
     * @param taxableIncome Taxable income amount
     * @return Calculated gross tax
     */
    private double calculateProgressiveTax(double taxableIncome) {
        if (taxableIncome <= 0) {
            return 0;
        }

        double tax = 0;

        if (taxableIncome <= HIGH_INCOME_THRESHOLD) {
            // Standard rate for income up to threshold
            tax = taxableIncome * STANDARD_TAX_RATE;
        } else {
            // Progressive taxation for high earners
            tax = HIGH_INCOME_THRESHOLD * STANDARD_TAX_RATE;
            double excessIncome = taxableIncome - HIGH_INCOME_THRESHOLD;
            tax += excessIncome * HIGH_INCOME_RATE;
        }

        return Math.round(tax * 100.0) / 100.0;
    }

    /**
     * Calculate total income from records
     *
     * @param records Income records
     * @return Total income amount
     */
    private double calculateTotalIncome(List<IncomeRecord> records) {
        return records.stream()
                .mapToDouble(IncomeRecord::getIncomeAmount)
                .sum();
    }

    /**
     * Calculate total WHT from records
     *
     * @param records Income records
     * @return Total WHT amount
     */
    private double calculateTotalWHT(List<IncomeRecord> records) {
        return records.stream()
                .mapToDouble(IncomeRecord::getWhtAmount)
                .sum();
    }

    /**
     * Get detailed calculation breakdown as formatted string
     *
     * @param validRecords Income records
     * @return Formatted calculation details
     */
    public String getCalculationDetails(List<IncomeRecord> validRecords) {
        if (validRecords == null || validRecords.isEmpty()) {
            return "No valid records available for calculation.";
        }

        TaxCalculationResult result = performDetailedTaxCalculation(validRecords);

        StringBuilder details = new StringBuilder();
        details.append("TAX CALCULATION BREAKDOWN\n");
        details.append("========================\n\n");

        details.append("INPUT DATA:\n");
        details.append(String.format("  Number of Records: %d\n", result.getRecordCount()));
        details.append(String.format("  Total Gross Income: Rs %s\n", CURRENCY_FORMAT.format(result.getTotalIncome())));
        details.append(String.format("  Total WHT Paid: Rs %s\n", CURRENCY_FORMAT.format(result.getTotalWHT())));
        details.append(String.format("  Average Income per Record: Rs %s\n", CURRENCY_FORMAT.format(result.getAverageIncomePerRecord())));
        details.append("\n");

        details.append("TAX CALCULATION:\n");
        details.append(String.format("  Tax-Free Allowance: Rs %s\n", CURRENCY_FORMAT.format(result.getTaxFreeAllowance())));
        details.append(String.format("  Taxable Income: Rs %s\n", CURRENCY_FORMAT.format(result.getTaxableIncome())));

        if (result.getTaxableIncome() > HIGH_INCOME_THRESHOLD) {
            double standardPortion = HIGH_INCOME_THRESHOLD * STANDARD_TAX_RATE;
            double highPortion = (result.getTaxableIncome() - HIGH_INCOME_THRESHOLD) * HIGH_INCOME_RATE;
            details.append(String.format("  Standard Rate Tax (12%%): Rs %s\n", CURRENCY_FORMAT.format(standardPortion)));
            details.append(String.format("  High Earner Tax (18%%): Rs %s\n", CURRENCY_FORMAT.format(highPortion)));
        } else {
            details.append(String.format("  Tax Rate Applied: %s%%\n", PERCENTAGE_FORMAT.format(STANDARD_TAX_RATE * 100)));
        }

        details.append(String.format("  Gross Tax Liability: Rs %s\n", CURRENCY_FORMAT.format(result.getGrossTax())));
        details.append(String.format("  Less: WHT Already Paid: Rs %s\n", CURRENCY_FORMAT.format(result.getTotalWHT())));
        details.append(String.format("  NET TAX PAYABLE: Rs %s\n", CURRENCY_FORMAT.format(result.getFinalTaxPayable())));
        details.append("\n");

        details.append("ANALYSIS:\n");
        details.append(String.format("  Effective Tax Rate: %s%%\n", PERCENTAGE_FORMAT.format(result.getEffectiveTaxRate())));
        details.append(String.format("  WHT Coverage of Tax: %s%%\n", PERCENTAGE_FORMAT.format(result.getWHTCoverage())));
        details.append(String.format("  Tax Savings from Allowance: Rs %s\n", CURRENCY_FORMAT.format(result.getTaxSavingsFromAllowance())));

        if (result.getFinalTaxPayable() <= 0) {
            details.append("\n  STATUS: No additional tax payable - WHT covers full liability\n");
        } else if (result.getWHTCoverage() > 80) {
            details.append("\n  STATUS: Low additional tax required - good WHT coverage\n");
        } else if (result.getWHTCoverage() < 50) {
            details.append("\n  STATUS: Significant additional tax required - consider increasing WHT\n");
        }

        return details.toString();
    }

    /**
     * Calculate optimal WHT strategy
     *
     * @param projectedIncome Projected annual income
     * @return Recommended WHT rate and amount
     */
    public WHTStrategy calculateOptimalWHT(double projectedIncome) {
        WHTStrategy strategy = new WHTStrategy();
        strategy.setProjectedIncome(projectedIncome);

        double taxableIncome = Math.max(0, projectedIncome - TAX_FREE_ALLOWANCE);
        double projectedTax = calculateProgressiveTax(taxableIncome);

        strategy.setProjectedTaxLiability(projectedTax);

        // Recommend WHT to cover 90% of tax liability
        double recommendedWHT = projectedTax * 0.90;
        double recommendedWHTRate = projectedIncome > 0 ? (recommendedWHT / projectedIncome) * 100 : 0;

        strategy.setRecommendedWHTAmount(recommendedWHT);
        strategy.setRecommendedWHTRate(recommendedWHTRate);

        // Calculate monthly WHT if paid monthly
        strategy.setMonthlyWHT(recommendedWHT / 12);

        return strategy;
    }

    /**
     * Analyze tax efficiency across different scenarios
     *
     * @param baseIncome Base income amount
     * @param scenarios Array of income variations to test
     * @return List of tax scenarios
     */
    public List<TaxScenario> analyzeTaxScenarios(double baseIncome, double[] scenarios) {
        List<TaxScenario> results = new ArrayList<>();

        for (double variation : scenarios) {
            double totalIncome = baseIncome + variation;

            TaxScenario scenario = new TaxScenario();
            scenario.setTotalIncome(totalIncome);
            scenario.setTaxableIncome(Math.max(0, totalIncome - TAX_FREE_ALLOWANCE));
            scenario.setGrossTax(calculateProgressiveTax(scenario.getTaxableIncome()));
            scenario.setNetTaxWithoutWHT(scenario.getGrossTax());
            scenario.setEffectiveRate(totalIncome > 0 ? (scenario.getGrossTax() / totalIncome) * 100 : 0);
            scenario.setMarginalRate(determineMarginalRate(totalIncome));

            results.add(scenario);
        }

        return results;
    }

    /**
     * Determine marginal tax rate for given income
     *
     * @param income Income amount
     * @return Marginal tax rate percentage
     */
    private double determineMarginalRate(double income) {
        double taxableIncome = Math.max(0, income - TAX_FREE_ALLOWANCE);

        if (taxableIncome <= 0) {
            return 0;
        } else if (taxableIncome <= HIGH_INCOME_THRESHOLD) {
            return STANDARD_TAX_RATE * 100;
        } else {
            return HIGH_INCOME_RATE * 100;
        }
    }

    /**
     * Generate tax compliance report
     *
     * @param records Income records
     * @return Compliance report
     */
    public String generateComplianceReport(List<IncomeRecord> records) {
        StringBuilder report = new StringBuilder();

        report.append("TAX COMPLIANCE REPORT\n");
        report.append("====================\n");
        report.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\n\n");

        if (records == null || records.isEmpty()) {
            report.append("No records available for compliance analysis.\n");
            return report.toString();
        }

        List<IncomeRecord> validRecords = records.stream()
                .filter(IncomeRecord::isValid)
                .collect(Collectors.toList());

        if (validRecords.isEmpty()) {
            report.append("No valid records found for compliance analysis.\n");
            return report.toString();
        }

        TaxCalculationResult result = performDetailedTaxCalculation(validRecords);

        report.append("COMPLIANCE STATUS:\n");
        report.append(String.format("  Records Processed: %d\n", validRecords.size()));
        report.append(String.format("  Total Declared Income: Rs %s\n", CURRENCY_FORMAT.format(result.getTotalIncome())));
        report.append(String.format("  Tax Liability: Rs %s\n", CURRENCY_FORMAT.format(result.getGrossTax())));
        report.append(String.format("  WHT Payments: Rs %s\n", CURRENCY_FORMAT.format(result.getTotalWHT())));
        report.append(String.format("  Outstanding Tax: Rs %s\n", CURRENCY_FORMAT.format(result.getFinalTaxPayable())));

        // Compliance indicators
        report.append("\nCOMPLIANCE INDICATORS:\n");

        if (result.getFinalTaxPayable() <= 0) {
            report.append("  ✓ Tax liability fully covered by WHT payments\n");
        } else {
            report.append("  ⚠ Additional tax payment required\n");
        }

        if (result.getWHTCoverage() >= 80) {
            report.append("  ✓ Good WHT coverage (≥80%)\n");
        } else {
            report.append("  ⚠ Low WHT coverage (<80%) - consider increasing WHT rate\n");
        }

        if (result.getEffectiveTaxRate() <= 15) {
            report.append("  ✓ Reasonable effective tax rate\n");
        } else {
            report.append("  ⚠ High effective tax rate - review tax planning strategies\n");
        }

        return report.toString();
    }

    /**
     * Get calculation history
     *
     * @return List of previous calculations
     */
    public List<TaxCalculationRecord> getCalculationHistory() {
        return new ArrayList<>(calculationHistory);
    }

    /**
     * Clear calculation history
     */
    public void clearHistory() {
        calculationHistory.clear();
    }

    /**
     * Log calculation summary to console
     *
     * @param result Calculation result
     */
    private void logCalculationSummary(TaxCalculationResult result) {
        System.out.println("Tax Calculation Summary:");
        System.out.println("  Records: " + result.getRecordCount());
        System.out.println("  Total Income: Rs " + CURRENCY_FORMAT.format(result.getTotalIncome()));
        System.out.println("  Tax Payable: Rs " + CURRENCY_FORMAT.format(result.getFinalTaxPayable()));
        System.out.println("  Effective Rate: " + PERCENTAGE_FORMAT.format(result.getEffectiveTaxRate()) + "%");
    }

    // Helper Classes
    public static class TaxCalculationResult {
        private int recordCount;
        private double totalIncome;
        private double totalWHT;
        private double taxFreeAllowance;
        private double taxableIncome;
        private double grossTax;
        private double finalTaxPayable;
        private double effectiveTaxRate;
        private double whtCoverage;
        private double averageIncomePerRecord;
        private double taxSavingsFromAllowance;

        // Getters and Setters
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

        public double getTotalIncome() { return totalIncome; }
        public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

        public double getTotalWHT() { return totalWHT; }
        public void setTotalWHT(double totalWHT) { this.totalWHT = totalWHT; }

        public double getTaxFreeAllowance() { return taxFreeAllowance; }
        public void setTaxFreeAllowance(double taxFreeAllowance) { this.taxFreeAllowance = taxFreeAllowance; }

        public double getTaxableIncome() { return taxableIncome; }
        public void setTaxableIncome(double taxableIncome) { this.taxableIncome = taxableIncome; }

        public double getGrossTax() { return grossTax; }
        public void setGrossTax(double grossTax) { this.grossTax = grossTax; }

        public double getFinalTaxPayable() { return finalTaxPayable; }
        public void setFinalTaxPayable(double finalTaxPayable) { this.finalTaxPayable = finalTaxPayable; }

        public double getEffectiveTaxRate() { return effectiveTaxRate; }
        public void setEffectiveTaxRate(double effectiveTaxRate) { this.effectiveTaxRate = effectiveTaxRate; }

        public double getWHTCoverage() { return whtCoverage; }
        public void setWHTCoverage(double whtCoverage) { this.whtCoverage = whtCoverage; }

        public double getAverageIncomePerRecord() { return averageIncomePerRecord; }
        public void setAverageIncomePerRecord(double averageIncomePerRecord) { this.averageIncomePerRecord = averageIncomePerRecord; }

        public double getTaxSavingsFromAllowance() { return taxSavingsFromAllowance; }
        public void setTaxSavingsFromAllowance(double taxSavingsFromAllowance) { this.taxSavingsFromAllowance = taxSavingsFromAllowance; }
    }

    public static class TaxCalculationRecord {
        private final LocalDateTime timestamp;
        private final int recordCount;
        private final TaxCalculationResult result;

        public TaxCalculationRecord(LocalDateTime timestamp, int recordCount, TaxCalculationResult result) {
            this.timestamp = timestamp;
            this.recordCount = recordCount;
            this.result = result;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public int getRecordCount() { return recordCount; }
        public TaxCalculationResult getResult() { return result; }
    }

    public static class WHTStrategy {
        private double projectedIncome;
        private double projectedTaxLiability;
        private double recommendedWHTAmount;
        private double recommendedWHTRate;
        private double monthlyWHT;

        // Getters and Setters
        public double getProjectedIncome() { return projectedIncome; }
        public void setProjectedIncome(double projectedIncome) { this.projectedIncome = projectedIncome; }

        public double getProjectedTaxLiability() { return projectedTaxLiability; }
        public void setProjectedTaxLiability(double projectedTaxLiability) { this.projectedTaxLiability = projectedTaxLiability; }

        public double getRecommendedWHTAmount() { return recommendedWHTAmount; }
        public void setRecommendedWHTAmount(double recommendedWHTAmount) { this.recommendedWHTAmount = recommendedWHTAmount; }

        public double getRecommendedWHTRate() { return recommendedWHTRate; }
        public void setRecommendedWHTRate(double recommendedWHTRate) { this.recommendedWHTRate = recommendedWHTRate; }

        public double getMonthlyWHT() { return monthlyWHT; }
        public void setMonthlyWHT(double monthlyWHT) { this.monthlyWHT = monthlyWHT; }
    }

    public static class TaxScenario {
        private double totalIncome;
        private double taxableIncome;
        private double grossTax;
        private double netTaxWithoutWHT;
        private double effectiveRate;
        private double marginalRate;

        // Getters and Setters
        public double getTotalIncome() { return totalIncome; }
        public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

        public double getTaxableIncome() { return taxableIncome; }
        public void setTaxableIncome(double taxableIncome) { this.taxableIncome = taxableIncome; }

        public double getGrossTax() { return grossTax; }
        public void setGrossTax(double grossTax) { this.grossTax = grossTax; }

        public double getNetTaxWithoutWHT() { return netTaxWithoutWHT; }
        public void setNetTaxWithoutWHT(double netTaxWithoutWHT) { this.netTaxWithoutWHT = netTaxWithoutWHT; }

        public double getEffectiveRate() { return effectiveRate; }
        public void setEffectiveRate(double effectiveRate) { this.effectiveRate = effectiveRate; }

        public double getMarginalRate() { return marginalRate; }
        public void setMarginalRate(double marginalRate) { this.marginalRate = marginalRate; }
    }
}
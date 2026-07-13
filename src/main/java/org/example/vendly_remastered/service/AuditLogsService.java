package org.example.vendly_remastered.service;

import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.models.entity.SaleHeader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

public class AuditLogsService {

    private static AuditLogsService instance;

    // Formatters for the console logs and file naming conventions
    private final DateTimeFormatter logTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private AuditLogsService() {}

    public static synchronized AuditLogsService getInstance() {
        if (instance == null) {
            instance = new AuditLogsService();
        }
        return instance;
    }

    // =========================================================================
    // 🎯 OVERLOADED LOGGING ENTRY POINTS
    // =========================================================================

    public synchronized void log(String type, String message) {
        writeToLedger(out -> {
            out.printf("[%s] | TYPE: %-10s | %s\n",
                    LocalDateTime.now().format(logTimestampFormatter),
                    type.toUpperCase(),
                    message);
        });
    }

    public synchronized void log(SaleHeader header, List<SaleDetailItem> items) {
        writeToLedger(out -> {
            out.println("================================================================================");
            out.printf("TIMESTAMP : %s | TRANSACTION ID: %d | TYPE: %s\n",
                    LocalDateTime.now().format(logTimestampFormatter),
                    header.getSaleId(),
                    header.getSaleNote().toUpperCase());
            out.printf("OPERATOR  : ID %d | TOTAL VALUE: %s DA | NET PROFIT: %s DA\n",
                    header.getSaleUser(),
                    header.getSaleTotal().toPlainString(),
                    header.getSaleProfit().toPlainString());
            out.println("ITEMS DETAILS:");

            for (SaleDetailItem item : items) {
                out.printf("  -> Product ID: %-5d | %-30s | Qty: %-6s | Price: %s DA\n",
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductQuantity().stripTrailingZeros().toPlainString(),
                        item.getProductUnitPrice().toPlainString());
            }
            out.println("================================================================================");
        });
    }

    public synchronized void log(int productId, String productName, double quantityDelta, String reason) {
        writeToLedger(out -> {
            String direction = quantityDelta >= 0 ? "STOCK_INCREASE" : "STOCK_DECREASE";
            out.printf("[%s] | %-14s | PROD #%d (%s) | ADJUSTMENT: %+.3f | REASON: %s\n",
                    LocalDateTime.now().format(logTimestampFormatter),
                    direction,
                    productId,
                    productName,
                    quantityDelta,
                    reason);
        });
    }

    // =========================================================================
    // 🧱 DYNAMIC DAILY FILE WRITER ENGINE
    // =========================================================================

    @FunctionalInterface
    private interface LogOperation {
        void execute(PrintWriter out) throws Exception;
    }

    /**
     * Dynamically computes the target filename based on the current system date
     * before running the requested log execution block.
     */
    private void writeToLedger(LogOperation operation) {
        // 📅 Dynamically compute the daily filename (e.g., "audit_2026_07_13.txt")
        String currentDateStr = LocalDate.now().format(fileDateFormatter);
        String dailyFileName = "audit_" + currentDateStr + ".txt";

        try (FileWriter fw = new FileWriter(dailyFileName, true);
             PrintWriter out = new PrintWriter(fw)) {

            operation.execute(out);
            out.println(); // Trailing blank line spacer

        } catch (IOException e) {
            System.err.println("[AuditLogsService] CRITICAL: Disk write violation on daily ledger: " + dailyFileName);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[AuditLogsService] Error executing internal daily logging operation.");
            e.printStackTrace();
        }
    }
}
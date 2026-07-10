package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Stock;
import org.example.vendly_remastered.repository.IStockRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StockSQLiteImpl implements IStockRepository {
    private Connection connection;

    public StockSQLiteImpl(Connection conn){ connection = conn; }

    @Override
    public Map<Integer, Stock> findAll() {
        Map<Integer, Stock> stockMap = new java.util.HashMap<>();
        String sql = "SELECT * FROM stock;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int productId = rs.getInt("product_id");

                // Safe extraction of text/numeric decimal representation
                String rawCurrentStock = rs.getString("current_stock");
                String rawMinThreshold = rs.getString("min_threshold");

                // stock is weak entity, can be accessed only vie the product
                Stock stock = new Stock(
                        rawCurrentStock != null ? new BigDecimal(rawCurrentStock) : BigDecimal.ZERO,
                        rawMinThreshold != null ? new BigDecimal(rawMinThreshold) : BigDecimal.ZERO
                );

                stockMap.put(productId, stock);
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to sweep complete inventory baseline map allocations.");
            e.printStackTrace();
        }
        return stockMap;
    }

    @Override
    public Stock getStock(int productId) {
        String sql = "SELECT current_stock, min_threshold FROM stock WHERE product_id = ?;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String rawCurrentStock = rs.getString("current_stock");
                    String rawMinThreshold = rs.getString("min_threshold");

                    // we bind things at the control level namely thr cache since it's the one calling all this
                    return new Stock(
                            rawCurrentStock != null ? new BigDecimal(rawCurrentStock) : BigDecimal.ZERO,
                            rawMinThreshold != null ? new BigDecimal(rawMinThreshold) : BigDecimal.ZERO
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to pull individual stock allocation line for product ID: " + productId);
            e.printStackTrace();
        }
        return null; // Safe fallback cache miss signifier
    }

    @Override
    public Map<Integer, BigDecimal> updateStockBatch(Map<Integer, Stock> stockUpdates) {
        Map<Integer, BigDecimal> updatedBalances = new java.util.HashMap<>();
        if (stockUpdates == null || stockUpdates.isEmpty()) return updatedBalances;

        String sql = """
            UPDATE stock 
            SET current_stock = current_stock - ?, last_update = CURRENT_TIMESTAMP
            WHERE product_id = ?
            RETURNING product_id, current_stock;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // 1. Queue all mutations into a single database batch execution path
            for (Map.Entry<Integer, Stock> entry : stockUpdates.entrySet()) {
                int productId = entry.getKey();
                Stock stock = entry.getValue();

                pstmt.setBigDecimal(1, stock.getCurrentStock()); // Amount sold
                pstmt.setInt(2, productId);
                pstmt.addBatch();
            }

            // 2. Transmit the entire execution array down to SQLite in one single trip
            pstmt.executeBatch();

            // 3. Harvest the RETURNING results from the batch pipeline
            try (ResultSet rs = pstmt.getGeneratedKeys()) { // Note: some JDBC drivers route RETURNING via getGeneratedKeys for batch updates
                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String rawStock = rs.getString("current_stock");
                    updatedBalances.put(id, rawStock != null ? new BigDecimal(rawStock) : BigDecimal.ZERO);
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Bulk batch stock mutation sequence failed.");
            e.printStackTrace();
        }

        return updatedBalances; // Map of productIds and their final absolute balances on disk
    }

    @Override
    public void addStock(int productId, Stock stock) {

    }

    @Override
    public void addStockBulk(Map<Integer, Stock> itemsInInvoice, String deliveryNotes) {
        if (itemsInInvoice == null || itemsInInvoice.isEmpty()) return;

        // Static, clean SQL layout. Parameter markers remain low to protect against SQLite variable bounds.
        String sql = """
            INSERT INTO stock (product_id, current_stock, min_threshold, last_update)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(product_id) DO UPDATE SET
                current_stock = current_stock + excluded.current_stock,
                min_threshold = excluded.min_threshold,
                last_update = CURRENT_TIMESTAMP;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // 1. Loop and stage parameters inside JVM memory
            for (Map.Entry<Integer, Stock> entry : itemsInInvoice.entrySet()) {
                int productId = entry.getKey();
                Stock stock = entry.getValue();

                pstmt.setInt(1, productId);
                pstmt.setBigDecimal(2, stock.getCurrentStock()); // The new quantity being added
                pstmt.setBigDecimal(3, stock.getMinThreshold());

                pstmt.addBatch(); // Enqueue parameters to the internal execution matrix array
            }

            // 2. Stream the data matrix to SQLite in a single transaction round-trip block
            pstmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("CRITICAL: High-performance bulk inventory counter adjustment sequence failed.");
            e.printStackTrace();
            // Propagate upstream to force transactional rollback on the shared database connection context
            throw new RuntimeException(e);
        }
    }

}

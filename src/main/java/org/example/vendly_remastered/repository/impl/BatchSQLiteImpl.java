package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Batch;
import org.example.vendly_remastered.repository.IBatchRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


public class BatchSQLiteImpl implements IBatchRepository {
    private Connection connection;
    public BatchSQLiteImpl(Connection conn){ connection = conn; }
    @Override
    public void saveAll(List<Batch> batches) {
        if (batches == null || batches.isEmpty()) {
            return;
        }

        // Filter out items without an expiry date (only alertable items create batch cards)
        List<Batch> alertableBatches = batches.stream()
                .filter(b -> b.getExpiryDate() != null)
                .toList();

        if (alertableBatches.isEmpty()) {
            return; // Nothing to track for expiry alerts
        }

        String sql = """
            INSERT INTO batch (product_id, batch_number, initial_received_qty, expiry_date)
            VALUES (?, ?, ?, ?);
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Batch batch : alertableBatches) {
                pstmt.setInt(1, batch.getProductId());

                String batchNum = (batch.getBatchNumber() != null && !batch.getBatchNumber().isBlank())
                        ? batch.getBatchNumber()
                        : "GENERIC";
                pstmt.setString(2, batchNum);

                pstmt.setBigDecimal(3, batch.getInitialReceivedQty() != null ? batch.getInitialReceivedQty() : BigDecimal.ZERO);
                pstmt.setDate(4, Date.valueOf(batch.getExpiryDate()));

                pstmt.addBatch();
            }

            pstmt.executeBatch();

        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to save batch expiry cards to disk.");
            e.printStackTrace();
            throw new RuntimeException("Batch insert failure", e);
        }
    }
}

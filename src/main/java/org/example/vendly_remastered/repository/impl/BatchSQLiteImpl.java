package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Batch;
import org.example.vendly_remastered.repository.IBatchrepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


public class BatchSQLiteImpl implements IBatchrepository {
    private Connection connection;
    public BatchSQLiteImpl(Connection conn){ connection = conn; }
    @Override
    public void saveAll(List<Batch> batches) {
        String sql = """
                INSERT INTO batch (product_id, batch_number, initial_received_qty, expiry_date)
                VALUES (?, ?, ?, ?);
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Batch batch : batches) {
                pstmt.setInt(1, batch.getProductId());
                pstmt.setString(2, batch.getBatchNumber());
                pstmt.setBigDecimal(3, batch.getInitialReceivedQty());
                pstmt.setDate(4, Date.valueOf(batch.getExpiryDate())); // Safe LocalDate conversion
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to save batch audit records to disk.");
            e.printStackTrace();
            throw new RuntimeException(e); // Propagate to trigger transaction rollback
        }
    }
}

package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Batch;
import org.example.vendly_remastered.repository.IBatchRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
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

    @Override
    public List<Batch> getExpiringSoon() {
        String sql = """
        SELECT P.product_name, B.* FROM batch B
        JOIN product P ON P.product_id = B.product_id
        WHERE B.expiry_date < date('now', '+10 days')
        ORDER BY B.expiry_date ASC;
        """;
        List<Batch> resultList = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Batch e = new Batch();
                e.setBatchId(rs.getInt("batch_id"));
                e.setProductId(rs.getInt("product_id"));
                e.setProductName(rs.getString("product_name"));
                // Assuming expiry_date is stored as text (YYYY-MM-DD) in SQLite and parsed into LocalDate
                String expiryDateStr = rs.getString("expiry_date");

                if (expiryDateStr != null) {
                    e.setExpiryDate(java.time.LocalDate.parse(expiryDateStr));
                }

                resultList.add(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultList;
    }

    @Override
    public void removeExpired() {
        String sql = "DELETE FROM batch WHERE expiry_date < date('now', '-5 days')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Removed " + rowsAffected + " expired batches older than 5 days.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

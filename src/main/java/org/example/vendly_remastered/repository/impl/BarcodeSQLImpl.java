package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.repository.IBarcodeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class BarcodeSQLImpl implements IBarcodeRepository {
    Connection connection;
    public BarcodeSQLImpl(Connection conn){ connection = conn; }
    @Override
    public void insertBarcode(Map<String, Integer> barcodeIdMap) {
        if (barcodeIdMap == null || barcodeIdMap.isEmpty()) {
            return;
        }

        // INSERT OR REPLACE overwrites existing entries on unique/primary key conflicts
        String sql = """
                INSERT OR REPLACE INTO product_barcode (product_id, barcode) 
                VALUES (?, ?);
                """;

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (Map.Entry<String, Integer> entry : barcodeIdMap.entrySet()) {
                    pstmt.setInt(1, entry.getValue());
                    pstmt.setString(2, entry.getKey());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new RuntimeException("Failed to batch insert/replace barcodes", e);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                // Log or handle auto-commit reset failure
            }
        }
    }

    @Override
    public void deleteBarcode(List<String> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) {
            return;
        }

        String sql = """
                DELETE FROM product_barcode 
                WHERE barcode = ?;
                """;

        boolean originalAutoCommit = true;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (String barcode : barcodes) {
                    pstmt.setString(1, barcode);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new RuntimeException("Failed to batch delete barcodes", e);
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException e) {
                // Log or handle auto-commit reset failure
            }
        }
    }
}

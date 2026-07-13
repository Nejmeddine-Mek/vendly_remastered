package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.repository.ISaleDetailsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SaleDetailsSQLiteImpl implements ISaleDetailsRepository {
    Connection connection;
    public SaleDetailsSQLiteImpl(Connection conn){ connection = conn; }
    @Override
    public void saveSaleDetails(List<SaleDetailItem> soldItems) {
        if (soldItems == null || soldItems.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO sale_details (sale_id, product_id, product_name, product_quantity, product_unit_price)
            VALUES (?, ?, ?, ?, ?);
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // 1. Loop and stage each line item into memory batches
            for (SaleDetailItem item : soldItems) {
                pstmt.setInt(1, item.getSaleId());
                pstmt.setInt(2, item.getProductId());
                pstmt.setString(3, item.getProductName());
                pstmt.setBigDecimal(4, item.getProductQuantity());
                pstmt.setBigDecimal(5, item.getProductUnitPrice());

                pstmt.addBatch(); // Staged
            }

            // 2. Blast the entire batch to the database engine at once
            int[] result = pstmt.executeBatch();
            System.out.println("Database batch execution completed. Rows affected: " + result.length);

        } catch (SQLException sqle) {
            System.err.println("CRITICAL ERROR: Failed to execute batch insert for sale detail line items.");
            sqle.printStackTrace();
        }
    }
}

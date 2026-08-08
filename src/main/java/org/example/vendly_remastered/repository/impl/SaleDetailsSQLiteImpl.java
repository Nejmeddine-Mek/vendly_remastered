package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.repository.ISaleDetailsRepository;
import org.example.vendly_remastered.service.ErrorLogsService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

        }catch (SQLException sqle) {
            String message = "CRITICAL ERROR: Failed to execute batch insert for sale detail line items.";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }
    }

    @Override
    public List<SaleDetailItem> getSaleDetails(int saleId) {
        String sql = """
        SELECT *
        FROM sale_details D WHERE D.sale_id = ?;
        """;

        List<SaleDetailItem> details = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, saleId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    details.add(mapRowToSaleDetailItem(rs));
                }
            }
        }catch (SQLException e) {
            String message = "CRITICAL ERROR: Failed to retrieve sale details";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, e);
        }

        return details;
    }

    private SaleDetailItem mapRowToSaleDetailItem(ResultSet rs) throws SQLException {
        SaleDetailItem item = new SaleDetailItem();
        item.setProductId(rs.getInt("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setProductQuantity(rs.getBigDecimal("product_quantity"));
        item.setProductUnitPrice(rs.getBigDecimal("product_unit_price"));
        return item;
    }
}

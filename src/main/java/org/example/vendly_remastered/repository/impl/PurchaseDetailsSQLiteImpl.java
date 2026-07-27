package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.PurchaseDetailItem;
import org.example.vendly_remastered.repository.IPurchaseDetailRepository;

import java.sql.Connection;
import java.util.List;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class PurchaseDetailsSQLiteImpl implements IPurchaseDetailRepository {

    private final Connection connection;

    public PurchaseDetailsSQLiteImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<PurchaseDetailItem> getPurchaseItems(int purchaseId) {
        String sql = """
                SELECT pd.purchase_id,
                       pd.product_id,
                       pd.item_no,
                       pd.unit_cost,
                       pd.quantity,
                       p.designation AS product_name
                FROM purchase_details pd
                LEFT JOIN products p ON pd.product_id = p.product_id
                WHERE pd.purchase_id = ?
                ORDER BY pd.item_no ASC;
                """;

        List<PurchaseDetailItem> items = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, purchaseId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PurchaseDetailItem item = new PurchaseDetailItem();
                    item.setPurchaseId(rs.getLong("purchase_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setItemNumber(rs.getInt("item_no"));
                    item.setUnitCost(rs.getBigDecimal("unit_cost"));
                    item.setQuantity(rs.getBigDecimal("quantity"));
                    item.setProductName(rs.getString("product_name"));

                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to fetch purchase items for purchase_id: " + purchaseId);
            e.printStackTrace();
        }

        return items;
    }

    @Override
    public void savePurchaseItems(List<PurchaseDetailItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO purchase_details (purchase_id, product_id, item_no, unit_cost, quantity)
                VALUES (?, ?, ?, ?, ?);
                """;

        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (PurchaseDetailItem item : items) {
                    pstmt.setLong(1, item.getPurchaseId());
                    pstmt.setLong(2, item.getProductId());
                    pstmt.setInt(3, item.getItemNumber() != null ? item.getItemNumber() : 1);
                    pstmt.setBigDecimal(4, safe(item.getUnitCost()));
                    pstmt.setBigDecimal(5, safe(item.getQuantity()));

                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                System.err.println("CRITICAL: Transaction rolled back during purchase details insert.");
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to batch save purchase detail items.");
            e.printStackTrace();
        }
    }

    // --- HELPER METHOD ---

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.IPurchaseHeaderRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PurchaseHeaderSQLiteImpl implements IPurchaseHeaderRepository {

    private final Connection connection;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PurchaseHeaderSQLiteImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int savePurchaseHeader(PurchaseHeader purchase) {
        String sql = """
                INSERT INTO purchase_header (supplier_id, user_id, purchase_cost, amount_paid, purchase_date)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, Math.toIntExact(purchase.getSupplierId()));
            pstmt.setInt(2, Math.toIntExact(purchase.getUserId()));
            pstmt.setBigDecimal(3, purchase.getPurchaseCost() != null ? purchase.getPurchaseCost() : BigDecimal.ZERO);
            pstmt.setBigDecimal(4, purchase.getAmountPaid() != null ? purchase.getAmountPaid() : BigDecimal.ZERO);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to save purchase header");
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<PurchaseHeader> findAll() {
        String sql = getBaseSelectQuery() + " ORDER BY p.purchase_date DESC;";
        List<PurchaseHeader> list = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToPurchaseHeader(rs));
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Error fetching all purchase headers");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<PurchaseHeader> findByDate(LocalDate date) {
        String sql = getBaseSelectQuery() + " WHERE DATE(p.purchase_date) = ? ORDER BY p.purchase_date DESC;";
        List<PurchaseHeader> list = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPurchaseHeader(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Error fetching purchase headers by date: " + date);
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<PurchaseHeader> getByRange(LocalDate beginning, LocalDate end) {
        String sql = getBaseSelectQuery() + " WHERE DATE(p.purchase_date) BETWEEN ? AND ? ORDER BY p.purchase_date DESC;";
        List<PurchaseHeader> list = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, beginning.toString());
            pstmt.setString(2, end.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPurchaseHeader(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Error fetching purchases by date range");
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<PurchaseHeader> getUnpaidPurchases(int supplierId) {
        String sql = getBaseSelectQuery() + " WHERE p.supplier_id = ? AND (p.purchase_cost - p.amount_paid) > 0 ORDER BY p.purchase_date ASC;";
        List<PurchaseHeader> list = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, supplierId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPurchaseHeader(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Error fetching unpaid purchases for supplier ID: " + supplierId);
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void updateHeaders(List<PurchaseHeader> purchaseHeaders) {
        String sql = """
                UPDATE purchase_header
                SET amount_paid = ?,
                    purchase_cost = ?
                WHERE purchase_id = ?;
                """;

        try {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (PurchaseHeader header : purchaseHeaders) {
                    pstmt.setBigDecimal(1, header.getAmountPaid());
                    pstmt.setBigDecimal(2, header.getPurchaseCost());
                    pstmt.setLong(3, header.getPurchaseId());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Batch update failed for purchase headers");
            e.printStackTrace();
        }
    }

    // --- HELPER METHODS ---

    private String getBaseSelectQuery() {
        return """
                SELECT p.purchase_id,
                       p.supplier_id,
                       p.user_id,
                       p.purchase_cost,
                       p.amount_paid,
                       p.purchase_date,
                       s.supplier_company AS supplier_name,
                       u.full_name AS user_name
                FROM purchase_header p
                LEFT JOIN suppliers s ON p.supplier_id = s.supplier_id
                LEFT JOIN users u ON p.user_id = u.user_id
                """;
    }

    private PurchaseHeader mapResultSetToPurchaseHeader(ResultSet rs) throws SQLException {
        PurchaseHeader header = new PurchaseHeader();
        header.setPurchaseId(rs.getLong("purchase_id"));
        header.setSupplierId(rs.getLong("supplier_id"));
        header.setUserId(rs.getLong("user_id"));
        header.setPurchaseCost(rs.getBigDecimal("purchase_cost"));
        header.setAmountPaid(rs.getBigDecimal("amount_paid"));

        String rawDate = rs.getString("purchase_date");
        if (rawDate != null) {
            try {
                header.setPurchaseDate(LocalDateTime.parse(rawDate, dateTimeFormatter));
            } catch (Exception e) {
                // Fallback for date-only formats or default timestamp parse
                header.setPurchaseDate(rs.getTimestamp("purchase_date").toLocalDateTime());
            }
        }

        header.setSupplierName(rs.getString("supplier_name"));
        header.setUserName(rs.getString("user_name"));

        return header;
    }
}

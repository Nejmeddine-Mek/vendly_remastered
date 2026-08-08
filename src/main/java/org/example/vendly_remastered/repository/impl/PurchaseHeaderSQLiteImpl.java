package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.repository.IPurchaseHeaderRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import java.sql.*;


public class PurchaseHeaderSQLiteImpl implements IPurchaseHeaderRepository {

    private final Connection connection;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PurchaseHeaderSQLiteImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public int savePurchaseHeader(PurchaseHeader purchase) {
        String sql = """
                INSERT INTO purchase_header (
                    supplier_id, user_id, invoice_number,
                    rc_number, nif_number, nis_number, art_number, cpa_account,
                    total_ht, discount_amount, tva_rate, tva_amount, stamp_duty,
                    purchase_cost, amount_paid, purchase_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);
                """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, purchase.getSupplierId());
            pstmt.setLong(2, purchase.getUserId());
            pstmt.setString(3, purchase.getInvoiceNumber());

            // Fiscal Snapshot
            pstmt.setString(4, purchase.getRcNumber());
            pstmt.setString(5, purchase.getNifNumber());
            pstmt.setString(6, purchase.getNisNumber());
            pstmt.setString(7, purchase.getArtNumber());
            pstmt.setString(8, purchase.getCpaAccount());

            // Financial & Tax Breakdown
            pstmt.setBigDecimal(9, safe(purchase.getTotalHt()));
            pstmt.setBigDecimal(10, safe(purchase.getDiscountAmount()));
            pstmt.setBigDecimal(11, safe(purchase.getTvaRate()));
            pstmt.setBigDecimal(12, safe(purchase.getTvaAmount()));
            pstmt.setBigDecimal(13, safe(purchase.getStampDuty()));

            // Totals & Settlement
            pstmt.setBigDecimal(14, safe(purchase.getPurchaseCost()));
            pstmt.setBigDecimal(15, safe(purchase.getAmountPaid()));

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
                    pstmt.setBigDecimal(1, safe(header.getAmountPaid()));
                    pstmt.setBigDecimal(2, safe(header.getPurchaseCost()));
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
                       p.invoice_number,
                       p.rc_number,
                       p.nif_number,
                       p.nis_number,
                       p.art_number,
                       p.cpa_account,
                       p.total_ht,
                       p.discount_amount,
                       p.tva_rate,
                       p.tva_amount,
                       p.stamp_duty,
                       p.purchase_cost,
                       p.amount_paid,
                       p.purchase_date,
                       s.supplier_company AS supplier_name,
                       u.full_name AS user_name
                FROM purchase_header p
                LEFT JOIN suppliers s ON p.supplier_id = s.supplier_id
                LEFT JOIN users u ON p.user_id = u.userId
                """;
    }

    private PurchaseHeader mapResultSetToPurchaseHeader(ResultSet rs) throws SQLException {
        PurchaseHeader header = new PurchaseHeader();
        header.setPurchaseId(rs.getLong("purchase_id"));
        header.setSupplierId(rs.getLong("supplier_id"));
        header.setUserId(rs.getLong("user_id"));
        header.setInvoiceNumber(rs.getString("invoice_number"));

        // Fiscal Snapshot
        header.setRcNumber(rs.getString("rc_number"));
        header.setNifNumber(rs.getString("nif_number"));
        header.setNisNumber(rs.getString("nis_number"));
        header.setArtNumber(rs.getString("art_number"));
        header.setCpaAccount(rs.getString("cpa_account"));

        // Financials
        header.setTotalHt(rs.getBigDecimal("total_ht"));
        header.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        header.setTvaRate(rs.getBigDecimal("tva_rate"));
        header.setTvaAmount(rs.getBigDecimal("tva_amount"));
        header.setStampDuty(rs.getBigDecimal("stamp_duty"));
        header.setPurchaseCost(rs.getBigDecimal("purchase_cost"));
        header.setAmountPaid(rs.getBigDecimal("amount_paid"));

        // Date Parsing
        String rawDate = rs.getString("purchase_date");
        if (rawDate != null) {
            try {
                header.setPurchaseDate(LocalDateTime.parse(rawDate, dateTimeFormatter));
            } catch (Exception e) {
                Timestamp ts = rs.getTimestamp("purchase_date");
                if (ts != null) {
                    header.setPurchaseDate(ts.toLocalDateTime());
                }
            }
        }

        // Display Joined Fields
        header.setSupplierName(rs.getString("supplier_name"));
        header.setUserName(rs.getString("user_name"));

        return header;
    }

    private BigDecimal safe(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}

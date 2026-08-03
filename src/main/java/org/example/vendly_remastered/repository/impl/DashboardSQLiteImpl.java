package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.dto.ClientDashboardItem;
import org.example.vendly_remastered.models.dto.ProductDashboardItem;
import org.example.vendly_remastered.models.dto.RevenueDashboardItem;
import org.example.vendly_remastered.models.dto.SupplierDashboardItem;
import org.example.vendly_remastered.repository.IDashBoardRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DashboardSQLiteImpl implements IDashBoardRepository {
    Connection connection;
    public DashboardSQLiteImpl(Connection connection){ this.connection = connection; }
    @Override
    public List<ClientDashboardItem> getTopClients() {
        return getTopClients(null, null);
    }

    @Override
    public List<ClientDashboardItem> getTopClients(LocalDate beginningDate, LocalDate endDate) {
        // If no dates are provided, default to a wide range (or all-time query)
        boolean filterByDate = (beginningDate != null && endDate != null);

        String sql = filterByDate ? """
            SELECT
                C.client_id AS clientId,
                C.contact_full_name AS clientFullName,
                COUNT(SH.sale_id) AS ordersCount,
                COALESCE(SUM(SH.paid_amount), 0.0) AS totalSpent
            FROM clients C
            INNER JOIN sale_header SH ON C.client_id = SH.sale_client
            WHERE DATE(SH.sale_date) BETWEEN ? AND ?
            GROUP BY C.client_id, C.contact_full_name
            ORDER BY ordersCount DESC
            LIMIT 10;
            """ : """
            SELECT
                C.client_id AS clientId,
                C.contact_full_name AS clientFullName,
                COUNT(SH.sale_id) AS ordersCount,
                COALESCE(SUM(SH.paid_amount), 0.0) AS totalSpent
            FROM clients C
            INNER JOIN sale_header SH ON C.client_id = SH.sale_client
            GROUP BY C.client_id, C.contact_full_name
            ORDER BY ordersCount DESC
            LIMIT 10;
            """;

        List<ClientDashboardItem> items = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            if (filterByDate) {
                pstmt.setString(1, beginningDate.toString());
                pstmt.setString(2, endDate.toString());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ClientDashboardItem(
                            rs.getLong("clientId"),
                            rs.getString("clientFullName"),
                            rs.getInt("ordersCount"),
                            rs.getDouble("totalSpent")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // or log with logger
        }

        return items;
    }

    @Override
    public List<SupplierDashboardItem> getTopSuppliers() {
        return getTopSuppliers(null, null);
    }

    @Override
    public List<SupplierDashboardItem> getTopSuppliers(LocalDate beginningDate, LocalDate endDate) {
        boolean filterByDate = (beginningDate != null && endDate != null);

        String sql = filterByDate ? """
        SELECT 
            S.supplier_id AS supplierId, 
            S.supplier_full_name AS supplierFullName, 
            COUNT(PH.purchase_id) AS ordersCount, 
            COALESCE(SUM(PH.amount_paid), 0.0) AS totalSpent 
        FROM suppliers S 
        INNER JOIN purchase_header PH ON S.supplier_id = PH.supplier_id
        WHERE DATE(PH.purchase_date) BETWEEN ? AND ?
        GROUP BY S.supplier_id, S.supplier_full_name
        ORDER BY ordersCount DESC
        LIMIT 10;
        """ : """
        SELECT 
            S.supplier_id AS supplierId, 
            S.supplier_full_name AS supplierFullName, 
            COUNT(PH.purchase_id) AS ordersCount, 
            COALESCE(SUM(PH.amount_paid), 0.0) AS totalSpent 
        FROM suppliers S 
        INNER JOIN purchase_header PH ON S.supplier_id = PH.supplier_id
        GROUP BY S.supplier_id, S.supplier_full_name
        ORDER BY ordersCount DESC
        LIMIT 10;
        """;

        List<SupplierDashboardItem> items = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            if (filterByDate) {
                pstmt.setString(1, beginningDate.toString());
                pstmt.setString(2, endDate.toString());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new SupplierDashboardItem(
                            rs.getLong("supplierId"),
                            rs.getString("supplierFullName"),
                            rs.getInt("ordersCount"),
                            rs.getDouble("totalSpent")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    @Override
    public List<RevenueDashboardItem> getRevenue() {
        // Default to the last 10 days ending today
        LocalDate today = LocalDate.now();
        return getRevenue(today.minusDays(9), today);
    }

    @Override
    public List<RevenueDashboardItem> getRevenue(LocalDate beginningDate, LocalDate endDate) {
        if (beginningDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            beginningDate = today.minusDays(9);
            endDate = today;
        }

        String sql = """
            SELECT 
                DATE(SH.sale_date) AS saleDate,
                COALESCE(SUM(SH.paid_amount), 0.0) AS totalRevenue,
                COALESCE(COUNT(SH.sale_id), 0) AS totalPurchases,
                COALESCE(SUM(SH.sale_profit), 0) AS totalProfit
            FROM sale_header SH
            WHERE DATE(SH.sale_date) BETWEEN ? AND ?
            GROUP BY DATE(SH.sale_date)
            ORDER BY saleDate ASC;
            """;

        List<RevenueDashboardItem> items = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, beginningDate.toString());
            pstmt.setString(2, endDate.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("saleDate");
                    LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : null;

                    items.add(new RevenueDashboardItem(
                            date,
                            rs.getDouble("totalRevenue"),
                            rs.getDouble("totalPurchases"),
                            rs.getDouble("totalProfit")// mapped to double in your DTO
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    @Override
    public List<ProductDashboardItem> getTopProducts() {
        LocalDate today = LocalDate.now();
        return getTopProducts(today.minusDays(9), today);
    }

    @Override
    public List<ProductDashboardItem> getTopProducts(LocalDate beginningDate, LocalDate endDate) {
        boolean filterByDate = (beginningDate != null && endDate != null);

        String sql = filterByDate ? """
        SELECT 
            SD.product_id AS product_id,
            SD.product_name AS productName,
            SUM(SD.product_quantity * SD.product_unit_price) AS totalRevenue,
            SUM(SD.product_quantity) AS totalQuantity
        FROM sale_details SD 
        NATURAL JOIN sale_header SH
        WHERE DATE(SH.sale_date) BETWEEN ? AND ?
        GROUP BY SD.product_name
        ORDER BY totalQuantity DESC
        LIMIT 10;
        """ : """
        SELECT 
            SD.product_id AS product_id,
            SD.product_name AS productName,
            SUM(SD.product_quantity * SD.product_unit_price) AS totalRevenue,
            SUM(SD.product_quantity) AS totalQuantity
        FROM sale_details SD 
        NATURAL JOIN sale_header SH
        GROUP BY SD.product_name
        ORDER BY totalQuantity DESC
        LIMIT 10;
        """;

        List<ProductDashboardItem> items = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            if (filterByDate) {
                pstmt.setString(1, beginningDate.toString());
                pstmt.setString(2, endDate.toString());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ProductDashboardItem(
                            rs.getInt("product_id"),
                            rs.getString("productName"),
                            rs.getInt("totalQuantity"),
                            rs.getDouble("totalRevenue")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return items;
    }

    @Override
    public int getLowStockCount() {
        String sql = """
                SELECT COUNT(*) AS lowStock FROM stock S
                WHERE S.current_stock <= S.min_threshold;
                """;
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("lowStock");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getExpiringProductsCount() {
        String sql = """
                SELECT COUNT(*) AS expiring FROM batch
                WHERE DATE(expiry_date) BETWEEN DATE('now') AND DATE('now', '+10 days');
                """;
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("expiring");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }


}

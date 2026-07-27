package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.dto.ClientDashboardItem;
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

}

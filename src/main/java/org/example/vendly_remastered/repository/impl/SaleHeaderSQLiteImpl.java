package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaleHeaderSQLiteImpl implements ISalesHeaderRepository {
    private Connection connection;
    public SaleHeaderSQLiteImpl(Connection conn){ this.connection = conn; }

    @Override
    public int saveSaleHeader(SaleHeader sale) {
        // 1. Keep the SQL pure and simple. The JDBC driver handles key retrieval.
        String sql = """
            INSERT INTO sale_header (sale_total, paid_amount, sale_profit, sale_user, sale_client, sale_note)
            VALUES (?, ?, ?, ?, ?, ?);
            """;

        // 2. Pass Statement.RETURN_GENERATED_KEYS to instruct the driver to keep the key
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setBigDecimal(1, sale.getSaleTotal());
            pstmt.setBigDecimal(2, sale.getPaidAmount());
            pstmt.setBigDecimal(3, sale.getSaleProfit());
            pstmt.setInt(4, sale.getSaleUser());

            if (sale.getSaleClient() == null) {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(5, sale.getSaleClient());
            }
            pstmt.setString(6, sale.getSaleNote());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // 3. Safely extract the database-generated auto-increment ID
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        sale.setSaleId(generatedId); // Update object state
                        return generatedId;          // Return the actual ID
                    }
                }
            }
        } catch (SQLException sqle) {
            System.err.println("CRITICAL: Failed to insert sale header record.");
            sqle.printStackTrace();
        }

        return -1; // Indicates failure
    }

    @Override
    public List<SaleHeader> findAll() {
        // 🔄 Switched to LEFT JOIN so anonymous cash sales (NULL clients) aren't dropped!
        String sql = """
            SELECT S.*, U.username AS sale_username, C.company_name, C.contact_full_name FROM sale_header S
            JOIN users U ON U.userId = S.sale_user
            LEFT JOIN clients C ON S.sale_client = C.client_id;
            """;
        List<SaleHeader> salesList = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                salesList.add(mapRowToSaleHeader(rs));
            }
            return salesList; // Returns the fully loaded list populated with DB data

        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return new ArrayList<>(); // Return empty mutable list instead of List.of() to avoid immutable crashes down the line
    }

    public List<SaleHeader> findByDate(LocalDate date) {
        if (date == null) return new ArrayList<>();

        String sql = """
            SELECT 
                S.*, 
                U.username AS sale_username,
                C.company_name, 
                C.contact_full_name
            FROM sale_header S
            JOIN users U ON U.userId = S.sale_user
            LEFT JOIN clients C ON S.sale_client = C.client_id
            WHERE date(S.sale_date) = ?;
            """;

        List<SaleHeader> salesList = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    salesList.add(mapRowToSaleHeader(rs));
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return salesList;
    }

    private SaleHeader mapRowToSaleHeader(ResultSet rs) throws SQLException {
        SaleHeader sale = new SaleHeader();

        sale.setSaleId(rs.getInt("sale_id"));

        java.sql.Timestamp timestamp = rs.getTimestamp("sale_date");
        if (timestamp != null) {
            sale.setSaleDate(timestamp.toLocalDateTime());
        }

        sale.setSaleTotal(rs.getBigDecimal("sale_total"));
        sale.setPaidAmount(rs.getBigDecimal("paid_amount"));
        sale.setSaleProfit(rs.getBigDecimal("sale_profit"));
        sale.setSaleUser(rs.getInt("sale_user"));
        sale.setSaleNote(rs.getString("sale_note"));
        sale.setSaleUsername(rs.getString("sale_username"));

        int clientId = rs.getInt("sale_client");
        if (rs.wasNull()) {
            sale.setSaleClient(null);
            sale.setClientName("Cash Customer");
        } else {
            sale.setSaleClient(clientId);
            String company = rs.getString("company_name");
            String contactName = rs.getString("contact_full_name");

            // Display corporate name along with the contact person if available
            if (company != null && !company.isBlank()) {
                sale.setClientName(company + " (" + contactName + ")");
            } else {
                sale.setClientName(contactName);
            }
        }

        return sale;
    }
}

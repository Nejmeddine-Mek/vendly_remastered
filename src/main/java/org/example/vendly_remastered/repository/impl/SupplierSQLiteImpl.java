package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.ISuppliersRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierSQLiteImpl implements ISuppliersRepository {
    Connection connection;
    public SupplierSQLiteImpl(Connection conn){ connection = conn; }
    @Override
    public List<Supplier> findAll() {
        String sql = """
            SELECT S.*, U.username FROM suppliers S
            JOIN users U ON S.created_by = U.userId;
            """;

        List<Supplier> suppliers = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Supplier supplier = new Supplier();

                supplier.setSupplierId(rs.getLong("supplier_id"));
                supplier.setSupplierCompany(rs.getString("supplier_company"));
                supplier.setSupplierFullName(rs.getString("supplier_full_name"));
                supplier.setPhoneNumber(rs.getString("phone_number"));
                supplier.setEmail(rs.getString("email"));

                // Map timestamps to LocalDateTime
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    supplier.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    supplier.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                supplier.setCreatedBy(rs.getInt("created_by"));

                // Map numeric debt accurately to BigDecimal
                BigDecimal debts = rs.getBigDecimal("debts");
                supplier.setDebts(debts != null ? debts : BigDecimal.ZERO);

                supplier.setActive(rs.getBoolean("is_active"));

                // Optional: If your Supplier class holds the resolved creator username
                // supplier.setCreatedByUsername(rs.getString("username"));

                suppliers.add(supplier);
            }

        } catch (SQLException e) {
            System.err.println("CRITICAL: Error executing findAll in SuppliersDAO");
            e.printStackTrace();
        }

        return suppliers;
    }

    @Override
    public List<Supplier> findByName(String name) {
        return List.of();
    }

    @Override
    public int AddSupplier(Supplier supplier) {
        String sql = """
            INSERT INTO suppliers (
                supplier_company, 
                supplier_full_name, 
                phone_number, 
                email, 
                created_by, 
                debts, 
                is_active
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, supplier.getSupplierCompany());
            pstmt.setString(2, supplier.getSupplierFullName());
            pstmt.setString(3, supplier.getPhoneNumber());
            pstmt.setString(4, supplier.getEmail());

            // Defaults to user ID 1 if createdBy was not set on the entity
            pstmt.setInt(5, supplier.getCreatedBy() != null ? supplier.getCreatedBy() : 1);
            pstmt.setBigDecimal(6, supplier.getDebts());
            pstmt.setBoolean(7, supplier.isActive());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1); // Auto-generated supplier_id from SQLite
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Error inserting supplier into DB");
            e.printStackTrace();
        }

        return -1; // Failure fallback
    }

    @Override
    public void updateSupplier(Supplier supplier) {

    }
}

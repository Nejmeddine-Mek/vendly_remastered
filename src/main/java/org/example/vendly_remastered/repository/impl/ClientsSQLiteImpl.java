package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.repository.IClientsRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ClientsSQLiteImpl implements IClientsRepository {
    private Connection connection;
    public ClientsSQLiteImpl(Connection conn){ connection = conn; }

    @Override
    public List<Client> getByName(String name) {
        return List.of();
    }

    @Override
    public Optional<Client> get(int clientId) {
        String sql = """
            SELECT C.*, U.username, U.userId
            FROM clients C
            LEFT JOIN users U ON C.created_by = U.userId
            WHERE C.client_id = ?;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Retrieve nullable String values safely
                    String companyName = rs.getString("company_name");
                    String email = rs.getString("email");
                    String address = rs.getString("address");

                    // 2. Map Numeric DB values to BigDecimal
                    java.math.BigDecimal currentDebt = rs.getBigDecimal("current_debt");
                    if (currentDebt == null) {
                        currentDebt = java.math.BigDecimal.ZERO;
                    }

                    // 3. Convert SQLite Timestamp to java.time.LocalDateTime
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    java.time.LocalDateTime createdAt = (timestamp != null)
                            ? timestamp.toLocalDateTime()
                            : java.time.LocalDateTime.now();

                    // 4. Instantiate the complete Client model matching your fields
                    Client c = new Client(
                            rs.getInt("client_id"),
                            companyName == null ? "" : companyName,
                            rs.getString("contact_full_name"),
                            email == null ? "" : email,
                            rs.getString("phone"),
                            address == null ? "" : address,
                            true, // Defaulting isActive to true since it's omitted in the current schema
                            rs.getInt("userId"),
                            rs.getString("username"), // Pulls U.username mapped to createdByUsername
                            createdAt
                    );

                    return Optional.of(c);
                }
            }
        } catch (SQLException sqle) {
            System.err.println("CRITICAL: Failed to query client profile with ID: " + clientId);
            sqle.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public boolean add(Client client) {
        return false;
    }

    @Override
    public boolean update(Client client) {
        return false;
    }

    @Override
    public boolean delete(int clientId) {
        return false;
    }

    @Override
    public List<Client> findAll() {
        return List.of();
    }
}

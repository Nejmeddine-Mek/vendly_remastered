package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.repository.IClientsRepository;
import org.example.vendly_remastered.service.ErrorLogsService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientsSQLiteImpl implements IClientsRepository {
    private Connection connection;
    public ClientsSQLiteImpl(Connection conn){ connection = conn; }

    @Override
    public List<Client> getByName(String name) {
        String sql = """
            SELECT C.*, U.username, U.userId
            FROM clients C
            LEFT JOIN users U ON C.created_by = U.userId
            WHERE c.is_active = 1
            AND LOWER(C.contact_full_name) like ?;
            """;
        List<Client> clientMatches = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String searchStr = "%" + name + "%";
            pstmt.setString(1, searchStr);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
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
                    c.setCurrentDebts(rs.getBigDecimal("current_debt"));
                    clientMatches.add(c);
                }
            }
        }catch (SQLException sqle) {
            String message = "CRITICAL: Failed to query client profile with Name like: " + name;
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return clientMatches;
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
                    c.setCurrentDebts(rs.getBigDecimal("current_debt"));
                    return Optional.of(c);
                }
            }
        } catch (SQLException sqle) {
            String message = "CRITICAL: Failed to query client profile with ID: " + clientId;
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return Optional.empty();
    }

    @Override
    public boolean add(Client client) {
        if (client == null) {
            return false;
        }

        String sql = """
            INSERT INTO clients (company_name, contact_full_name, email, phone, address, current_debt, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?);
            """;

        // Pass Statement.RETURN_GENERATED_KEYS to instruct SQLite to track the auto-increment value
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 1. Handle Nullable / Optional Fields
            if (client.getCompanyName() == null || client.getCompanyName().isBlank()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, client.getCompanyName().trim());
            }

            pstmt.setString(2, client.getContactFullName().trim());

            if (client.getEmail() == null || client.getEmail().isBlank()) {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(3, client.getEmail().trim());
            }

            pstmt.setString(4, client.getPhone().trim());

            if (client.getAddress() == null || client.getAddress().isBlank()) {
                pstmt.setNull(5, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(5, client.getAddress().trim());
            }

            // Set current debt (defaulting to zero if null)
            if (client.getCurrentDebts() == null) {
                pstmt.setBigDecimal(6, java.math.BigDecimal.ZERO);
            } else {
                pstmt.setBigDecimal(6, client.getCurrentDebts());
            }

            // Set created_by user reference id (system audits)
            if (client.getCreatedBy() == 0) {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(7, client.getCreatedBy());
            }

            // 2. Execute SQL Write Statement
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // 3. Extract the SQLite-generated client_id and sync the Java model
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        client.setClientId(generatedId); // Crucial: Enables cache and details mappings to link correctly!
                        return true;
                    }
                }
            }

        }catch (SQLException sqle) {
            String message = "CRITICAL ERROR: Failed to add new client record: " + client.getContactFullName();
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return false;
    }

    @Override
    public boolean update(Client client) {
        if (client == null || client.getClientId() == 0) {
            return false;
        }

        String sql = """
        UPDATE clients 
        SET company_name = ?, 
            contact_full_name = ?, 
            email = ?, 
            phone = ?, 
            address = ?, 
            current_debt = ?,
            is_active = ?
        WHERE client_id = ?;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            // 1. Handle Nullable / Optional Fields consistently with save()
            if (client.getCompanyName() == null || client.getCompanyName().isBlank()) {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(1, client.getCompanyName().trim());
            }

            pstmt.setString(2, client.getContactFullName().trim());

            if (client.getEmail() == null || client.getEmail().isBlank()) {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(3, client.getEmail().trim());
            }

            pstmt.setString(4, client.getPhone().trim());

            if (client.getAddress() == null || client.getAddress().isBlank()) {
                pstmt.setNull(5, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(5, client.getAddress().trim());
            }

            // Set current debt tracking
            if (client.getCurrentDebts() == null) {
                pstmt.setBigDecimal(6, java.math.BigDecimal.ZERO);
            } else {
                pstmt.setBigDecimal(6, client.getCurrentDebts());
            }
            pstmt.setInt(7,client.isActive() ? 1:0);
            // 2. Identify target row via Primary Key ID
            pstmt.setInt(8, client.getClientId());

            // 3. Execute update statement
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        }catch (SQLException sqle) {
            String message = "CRITICAL ERROR: Failed to update client record ID [" + client.getClientId() + "]: " + client.getContactFullName();
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return false;
    }

    @Deprecated
    @Override
    public boolean delete(int clientId) {
        return false;
    }

    @Override
    public List<Client> findAll() {
        String sql = """
            SELECT C.*,
                   U.username AS created_by_username, U.userId AS created_by_id
            FROM clients C
            JOIN users U ON C.created_by = U.userId
            """;

        List<Client> clients = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Client client = new Client();
                client.setClientId(rs.getInt("client_id"));
                client.setCompanyName(rs.getString("company_name") == null ? "":rs.getString("company_name"));
                client.setContactFullName(rs.getString("contact_full_name"));
                client.setEmail(rs.getString("email") == null ? "":rs.getString("email"));
                client.setPhone(rs.getString("phone"));
                client.setAddress(rs.getString("address") == null ? "":rs.getString("address"));
                client.setActive(rs.getBoolean("is_active"));
                client.setCreatedBy(rs.getInt("created_by"));
                client.setCreatedByUsername(rs.getString("created_by_username"));
                client.setCreatedBy(rs.getInt("created_by_id"));
                client.setCurrentDebts(rs.getBigDecimal("current_debt"));
                // Handle timestamp mapping (java.sql.Timestamp to LocalDateTime / Date / String)
                if (rs.getTimestamp("created_at") != null) {
                    client.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }

                clients.add(client);
            }

        }catch (SQLException e) {
            String message = "CRITICAL ERROR: Failed to retrieve all client records";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, e);
        }

        return clients;
    }
}

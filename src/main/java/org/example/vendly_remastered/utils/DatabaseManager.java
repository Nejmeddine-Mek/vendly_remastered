package org.example.vendly_remastered.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:vendly.db"; // Your local file database
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }
    public static void initializeDatabase(){
        String createRolesTable = "CREATE TABLE IF NOT EXISTS roles (\n" +
                "    role_id SMALLINT PRIMARY KEY,\n" +
                "    role_name VARCHAR(32) NOT NULL UNIQUE,\n" +
                "    can_sell BOOLEAN DEFAULT TRUE,\n" +
                "    can_see_clients BOOLEAN DEFAULT FALSE,\n" +
                "    can_edit_clients BOOLEAN DEFAULT FALSE,\n" +
                "    can_see_products BOOLEAN DEFAULT FALSE,\n" +
                "    can_edit_products BOOLEAN DEFAULT FALSE,\n" +
                "    can_see_suppliers BOOLEAN DEFAULT FALSE,\n" +
                "    can_edit_suppliers BOOLEAN DEFAULT FALSE,\n" +
                "    can_see_history BOOLEAN DEFAULT FALSE,\n" +
                "    can_see_dashboard BOOLEAN DEFAULT FALSE\n" +
                ");";
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (\n" +
                "    userId SERIAL PRIMARY KEY,\n" +
                "    username VARCHAR(50) NOT NULL UNIQUE,\n" +
                "    password_hash VARCHAR(255) NOT NULL,\n" +
                "    full_name VARCHAR(100) NOT NULL,\n" +
                "    is_active BOOLEAN DEFAULT TRUE,\n" +
                "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    last_login TIMESTAMP,\n" +
                "    role SMALLINT,\n" +
                "    FOREIGN KEY (role) REFERENCES roles(role_id) ON DELETE RESTRICT\n" +
                ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createRolesTable);
            stmt.execute(createUsersTable);
            System.out.println("Database tables initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

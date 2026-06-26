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
        String createRolesTable = """
                CREATE TABLE IF NOT EXISTS roles (
                    role_id SMALLINT PRIMARY KEY,
                    role_name VARCHAR(32) NOT NULL UNIQUE,
                    can_sell BOOLEAN DEFAULT TRUE,
                    can_see_clients BOOLEAN DEFAULT FALSE,
                    can_edit_clients BOOLEAN DEFAULT FALSE,
                    can_see_products BOOLEAN DEFAULT FALSE,
                    can_edit_products BOOLEAN DEFAULT FALSE,
                    can_see_suppliers BOOLEAN DEFAULT FALSE,
                    can_edit_suppliers BOOLEAN DEFAULT FALSE,
                    can_see_history BOOLEAN DEFAULT FALSE,
                    can_see_dashboard BOOLEAN DEFAULT FALSE
                );
                """;
        String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    userId SERIAL PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_by INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_login TIMESTAMP,
                    role SMALLINT,
                    FOREIGN KEY (role) REFERENCES roles(role_id) ON DELETE RESTRICT,
                    FOREIGN KEY (created_by) REFERENCES users(userId) ON DELETE SET NULL
                );
                """;
        String createClientsTable = """
                CREATE TABLE IF NOT EXISTS clients (
                    client_id SERIAL PRIMARY KEY,
                    company_name VARCHAR(150),
                    contact_full_name VARCHAR(100) NOT NULL,
                    email VARCHAR(100),
                    phone VARCHAR(20) NOT NULL,
                    address TEXT,
                    total_purchases INT DEFAULT 0,       -- Maps to colPurchases
                    current_debt DECIMAL(12, 2) DEFAULT 0.00, -- Maps to colDebts
                    is_active BOOLEAN DEFAULT TRUE,
                    created_by INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (created_by) REFERENCES users(userId) ON DELETE SET NULL
                );
                """;
        String createSaleHeaderTable = """
                CREATE TABLE IF NOT EXISTS sale_header (
                    sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    sale_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                    sale_profit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                    sale_user INT NOT NULL,
                    sale_client INT, 
                    sale_note VARCHAR(32) NOT NULL DEFAULT 'sale',
                    FOREIGN KEY (sale_user) REFERENCES users(userId),
                    FOREIGN KEY (sale_client) REFERENCES clients(client_id) ON DELETE SET NULL,
                    
                    -- 🛡️ The fixed CHECK constraint for status types
                    CONSTRAINT chk_sale_type CHECK (sale_note IN ('sale', 'return', 'expired'))
                );
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createRolesTable);
            stmt.execute(createUsersTable);
            stmt.execute(createClientsTable);
            stmt.execute(createSaleHeaderTable);
            System.out.println("Database tables initialized successfully.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

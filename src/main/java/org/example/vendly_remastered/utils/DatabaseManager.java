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
    public static void initializeDatabase() {
        String createRolesTable = """
            CREATE TABLE IF NOT EXISTS roles (
                role_id INTEGER PRIMARY KEY AUTOINCREMENT,
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
                userId INTEGER PRIMARY KEY AUTOINCREMENT,
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
                client_id INTEGER PRIMARY KEY AUTOINCREMENT,
                company_name VARCHAR(150),
                contact_full_name VARCHAR(100) NOT NULL,
                email VARCHAR(100),
                phone VARCHAR(20) NOT NULL,
                address TEXT,
                total_purchases INT DEFAULT 0,
                current_debt DECIMAL(12, 2) DEFAULT 0.00,
                is_active BOOLEAN DEFAULT TRUE,
                created_by INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (created_by) REFERENCES users(userId) ON DELETE SET NULL
            );
            """;

        String createCategoryTable = """
            CREATE TABLE IF NOT EXISTS category (
                category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                category_name VARCHAR(64) NOT NULL UNIQUE
            );
            """;

        String createProductsTable = """
            CREATE TABLE IF NOT EXISTS product (
                product_id INTEGER PRIMARY KEY,
                product_name VARCHAR(64) NOT NULL,
                cost_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                retail_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
                created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_on TIMESTAMP DEFAULT NULL,
                category SMALLINT NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT TRUE,
                FOREIGN KEY (category) REFERENCES category(category_id),
                CONSTRAINT chck_price CHECK (retail_price >= cost_price)
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
                CONSTRAINT chk_sale_type CHECK (sale_note IN ('sale', 'return', 'expired'))
            );
            """;

        String createBarcodeIdTable = """
            CREATE TABLE IF NOT EXISTS product_barcode (
                barcode VARCHAR(24) PRIMARY KEY,
                product_id INT NOT NULL,
                FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
            );
            """;

        String createStockTable = """
            CREATE TABLE IF NOT EXISTS stock (
                product_id INT PRIMARY KEY,
                current_stock NUMERIC(10, 3) NOT NULL DEFAULT 0.000,
                min_threshold NUMERIC(10, 3) DEFAULT 5.000,
                last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
            );
            """;

        String createBatchTable = """
            CREATE TABLE IF NOT EXISTS batch (
                batch_id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INT NOT NULL,
                batch_number VARCHAR(32) NOT NULL DEFAULT 'GENERIC',
                initial_received_qty NUMERIC(10, 3) NOT NULL,
                expiry_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enforce strict ordered sequential parsing execution to prevent foreign key errors
            stmt.execute(createRolesTable);
            stmt.execute(createUsersTable);
            stmt.execute(createClientsTable);
            stmt.execute(createCategoryTable);
            stmt.execute(createProductsTable);
            stmt.execute(createSaleHeaderTable);
            stmt.execute(createBarcodeIdTable);
            stmt.execute(createStockTable);
            stmt.execute(createBatchTable);

            System.out.println("Vendly Remastered DB Schema initialized safely.");

        } catch (SQLException e) {
            System.err.println("CRITICAL FAILURE: Schema execution rejected during DB bootstrap.");
            e.printStackTrace();
        }
    }
}

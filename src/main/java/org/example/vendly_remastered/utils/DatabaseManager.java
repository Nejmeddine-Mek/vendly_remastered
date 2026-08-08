package org.example.vendly_remastered.utils;

import org.example.vendly_remastered.service.ErrorLogsService;

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
        can_see_dashboard BOOLEAN DEFAULT FALSE,
        can_see_settings BOOLEAN DEFAULT TRUE,
        can_see_users BOOLEAN DEFAULT FALSE,
        can_see_purchases BOOLEAN DEFAULT FALSE,
        can_edit_purchases BOOLEAN DEFAULT FALSE
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

        String createSuppliersTable = """
        CREATE TABLE IF NOT EXISTS suppliers (
            supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
            supplier_company VARCHAR(64) NOT NULL,
            supplier_full_name VARCHAR(64) NOT NULL,
            phone_number VARCHAR(32) NOT NULL,
            email VARCHAR(32),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_by INTEGER NOT NULL,
            debts NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            is_active BOOLEAN DEFAULT TRUE,
            FOREIGN KEY(created_by) REFERENCES users(userId) ON DELETE CASCADE
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
            product_name VARCHAR(64) NOT NULL UNIQUE,
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

        String createPromotionTable = """
                CREATE TABLE IF NOT EXISTS promotion (
                	product_id INTEGER PRIMARY KEY,
                	promo_quantity NUMERIC(10,2) NOT NULL,
                	promo_price NUMERIC(10,2) NOT NULL,
                FOREIGN KEY(product_id) REFERENCES product(product_id)
                
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
        String createSaleDetailsTable = """                
                CREATE TABLE IF NOT EXISTS sale_details (
                    sale_id INT NOT NULL,
                    product_id INT NOT NULL,
                    product_name VARCHAR(64) NOT NULL,
                    product_quantity NUMERIC(10, 3) NOT NULL,
                    product_unit_price NUMERIC(12, 2) NOT NULL,
                    PRIMARY KEY (sale_id, product_id),
                    FOREIGN KEY (sale_id) REFERENCES sale_header(sale_id) ON DELETE CASCADE,
                    FOREIGN KEY (product_id) REFERENCES product(product_id)
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

        String createPurchaseHeaderTable = """
        CREATE TABLE IF NOT EXISTS purchase_header (
            purchase_id INTEGER PRIMARY KEY AUTOINCREMENT,
            supplier_id INT NOT NULL REFERENCES suppliers(supplier_id),
            user_id INT NOT NULL REFERENCES users(userId),
            invoice_number VARCHAR(100),
            rc_number VARCHAR(50),
            nif_number VARCHAR(50),
            nis_number VARCHAR(50),
            art_number VARCHAR(50),
            cpa_account VARCHAR(50),
            total_ht NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            tva_rate NUMERIC(5,2) NOT NULL DEFAULT 19.00,
            tva_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            stamp_duty NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            purchase_cost NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            amount_paid NUMERIC(12,2) NOT NULL DEFAULT 0.00,
            remaining_debt NUMERIC(12,2) GENERATED ALWAYS AS (purchase_cost - amount_paid) STORED,
            purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        """;

        String createPurchaseDetailsTable = """
        CREATE TABLE IF NOT EXISTS purchase_details (
            purchase_id INT NOT NULL,
            product_id INT NOT NULL,
            cost_price NUMERIC(12,2) DEFAULT 0.00,
            quantity NUMERIC(12,2) DEFAULT 0,
            FOREIGN KEY (purchase_id) REFERENCES purchase_header(purchase_id) ON DELETE CASCADE,
            FOREIGN KEY (product_id) REFERENCES product(product_id) ON DELETE CASCADE,
            PRIMARY KEY (purchase_id, product_id)
        );
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Enforce strict ordered sequential execution to prevent foreign key errors
            stmt.execute(createRolesTable);
            stmt.execute(createUsersTable);
            stmt.execute(createClientsTable);
            stmt.execute(createSuppliersTable);         // Added: depends on users
            stmt.execute(createCategoryTable);
            stmt.execute(createProductsTable);
            stmt.execute(createPromotionTable);
            stmt.execute(createSaleHeaderTable);
            stmt.execute(createSaleDetailsTable);
            stmt.execute(createBarcodeIdTable);
            stmt.execute(createStockTable);
            stmt.execute(createBatchTable);
            stmt.execute(createPurchaseHeaderTable);    // Added: depends on suppliers & users
            stmt.execute(createPurchaseDetailsTable);   // Added: depends on purchase_header & product

            System.out.println("Vendly Remastered DB Schema initialized safely.");

        }catch (SQLException e) {
            String message = "CRITICAL FAILURE: Schema execution rejected during DB bootstrap.";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, e);
        }
    }

    /**
     * <strong>WARNING:</strong> Never call this method before {@link #initializeDatabase()}
     * has been successfully executed. It depends on the {@code roles} and {@code users}
     * tables existing, otherwise it will throw a {@link java.sql.SQLException}.
     */
    public static void createAdminAccount() {
        String insertRoleSql = """
    INSERT OR IGNORE INTO roles (
        role_id, role_name, can_sell, can_see_clients, can_edit_clients, 
        can_see_products, can_edit_products, can_see_suppliers, 
        can_edit_suppliers, can_see_history, can_see_dashboard,
        can_see_settings, can_see_users, can_see_purchases, can_edit_purchases
    ) VALUES (
        1, 'Administrator', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
    );
    """;

        String insertUserSql = """
    INSERT OR IGNORE INTO users (
        userId, username, password_hash, full_name, role, created_at
    ) VALUES (
        1, 'admin', '$2a$12$7jkkdKxt8A9TGzsRNA1cMe4j4mCf8XxKN6V2aOd5p8HBzE/aBsmOy', 'System Administrator', 1, CURRENT_TIMESTAMP
    );
    """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(insertRoleSql);
            stmt.execute(insertUserSql);
            System.out.println("Default admin role and account initialized successfully.");

        }catch (SQLException e) {
            String message = "Failed to create admin account and role.";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, e);
        }
    }
}

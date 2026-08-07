package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.repository.IArchiveRepository;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;

public class ArchiveSQLiteImpl implements IArchiveRepository {
    Connection connection;
    public ArchiveSQLiteImpl(Connection connection){ this.connection = connection; }
    @Override
    public int ArchiveSales(String path, LocalDate beginningDate, LocalDate endDate) {
        String createHeader = """
        CREATE TABLE IF NOT EXISTS sale_header (
            sale_id INTEGER PRIMARY KEY AUTOINCREMENT,
            sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            sale_total NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
            paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
            sale_profit NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
            sale_user INT NOT NULL,
            sale_client INT,
            sale_note VARCHAR(32) NOT NULL DEFAULT 'sale',
            CONSTRAINT chk_sale_type CHECK (sale_note IN ('sale', 'return', 'expired'))
        );
        """;

        String createItems = """
        CREATE TABLE IF NOT EXISTS sale_details (
            sale_id INT NOT NULL,
            product_id INT NOT NULL,
            product_name VARCHAR(64) NOT NULL,
            product_quantity NUMERIC(10, 3) NOT NULL,
            product_unit_price NUMERIC(12, 2) NOT NULL,
            PRIMARY KEY (sale_id, product_id)
        );
        """;

        String insertHeaders = """
        INSERT INTO sale_header
        SELECT * FROM sale_header 
        WHERE DATE(sale_date) BETWEEN ? AND ?;
        """;

        String insertDetails = """
        INSERT INTO sale_details
        SELECT * FROM sale_details 
        WHERE sale_id IN (
            SELECT sale_id FROM sale_header 
            WHERE DATE(sale_date) BETWEEN ? AND ?
        );
        """;
        String deleteHeaders = """
        DELETE FROM sale_header
        WHERE DATE(sale_date) BETWEEN ? AND ?;
        """;
        String deleteDetails = """
        DELETE FROM sale_details
        WHERE sale_id IN (
            SELECT sale_id FROM sale_header 
            WHERE DATE(sale_date) BETWEEN ? AND ?
        );
        """;
        String fileName = "Sales_Archive_" + beginningDate + "__" + endDate + ".db";
        File archiveFile = new File(path, fileName);
        String archiveDbUrl = "jdbc:sqlite:" + archiveFile.getAbsolutePath();

        try(Connection archiveConnection = DriverManager.getConnection(archiveDbUrl)){
            // Disable auto-commit for transaction atomicity
            connection.setAutoCommit(false);
            archiveConnection.setAutoCommit(false);
                // 1. Create tables in the archive database
                try (Statement stmt = archiveConnection.createStatement()) {
                    stmt.execute(createHeader);
                    stmt.execute(createItems);
                }
            String startStr = beginningDate.toString();
            String endStr = endDate.toString();

            try{try (PreparedStatement pstmt = archiveConnection.prepareStatement(insertHeaders)) {
                pstmt.setString(1, startStr);
                pstmt.setString(2, endStr);
                pstmt.executeUpdate();
            }

                // 3. Insert details into archive
                try (PreparedStatement pstmt = archiveConnection.prepareStatement(insertDetails)) {
                    pstmt.setString(1, startStr);
                    pstmt.setString(2, endStr);
                    pstmt.executeUpdate();
                }
                // 4. Delete details from main database first (due to foreign key dependencies)
                try (PreparedStatement pstmt = connection.prepareStatement(deleteDetails)) {
                    pstmt.setString(1, startStr);
                    pstmt.setString(2, endStr);
                    pstmt.executeUpdate();
                }

                // 5. Delete headers from main database
                try (PreparedStatement pstmt = connection.prepareStatement(deleteHeaders)) {
                    pstmt.setString(1, startStr);
                    pstmt.setString(2, endStr);
                    pstmt.executeUpdate();
                }
                // 6. Commit both transactions if everything succeeded
                archiveConnection.commit();
                connection.commit();

                return 1; // Success code
            } catch (Exception ex) {
                // Rollback both databases if any step fails
                try {
                    archiveConnection.rollback();
                } catch (SQLException rbEx) {
                    rbEx.printStackTrace();
                }
                try {
                    connection.rollback();
                } catch (SQLException rbEx) {
                    rbEx.printStackTrace();
                }
                ex.printStackTrace();
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }
}

package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ProductSQLiteImpl implements IProductRepository {
    private Connection connection;
    public ProductSQLiteImpl(Connection conn){ this.connection = conn; }
    @Override
    public List<Product> findAll() {
        // Fixed the table casing and typos (categrory_name, poduct_id)
        String sql = """
            SELECT 
                P.product_id,
                P.product_name,
                P.cost_price,
                P.retail_price,
                P.created_on,
                P.updated_on,
                P.category AS category_id,
                P.is_active,
                C.category_name AS category_name, 
                PB.barcode AS barcode 
            FROM product P
            JOIN category C ON C.category_id = P.category
            LEFT JOIN product_barcode PB ON P.product_id = PB.product_id;
            """;

        // LinkedHashMap preserves the database order while deduplicating rows by product_id
        Map<Integer, Product> productMap = new LinkedHashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int productId = rs.getInt("product_id");

                // Check if we already created this product object on a previous row
                Product product = productMap.get(productId);

                if (product == null) {
                    product = new Product();
                    product.setProductId(productId);
                    product.setProductName(rs.getString("product_name"));
                    product.setCostPrice(rs.getBigDecimal("cost_price"));
                    product.setRetailPrice(rs.getBigDecimal("retail_price"));

                    // Handle optional timestamps safely
                    if (rs.getTimestamp("created_on") != null) {
                        product.setCreatedOn(rs.getTimestamp("created_on").toLocalDateTime());
                    }
                    if (rs.getTimestamp("updated_on") != null) {
                        product.setUpdatedOn(rs.getTimestamp("updated_on").toLocalDateTime());
                    }

                    product.setCategoryId(rs.getShort("category_id"));
                    product.setActive(rs.getBoolean("is_active"));
                    product.setCategoryName(rs.getString("category_name"));

                    // Initialize the barcode tracker list
                    productMap.put(productId, product);
                }

                // Extract the barcode from the LEFT JOIN
                String barcodeStr = rs.getString("barcode");
                if (barcodeStr != null) {
                    product.getBarcodes().add(barcodeStr);

                    // Set the primary string field to the first barcode found for easy display
                    // this one might be removed at this point
                    if (product.getBarcode() == null) {
                        product.setBarcode(barcodeStr);
                    }
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            // Depending on your architecture, you might want to rethrow this as a runtime exception
        }

        // Return the unique list of products ready for the ObservableList wrapper
        return new ArrayList<>(productMap.values());
    }

    @Override
    public void update(Product processedProduct) {

    }

    @Override
    public void save(Product processedProduct) {

    }

    @Override
    public Optional<Product> findProduct(int productId) {
        return Optional.empty();
    }

    @Override
    public List<Product> findByName(String productName) {
        if (productName == null || productName.isBlank()) return new ArrayList<>();

        // 🛡️ The wildcards must be stripped out of the SQL layout text entirely
        String sql = """
            SELECT *
            FROM product p
            INNER JOIN category c ON p.category = c.category_id
            INNER JOIN product_barcode pb ON p.product_id = pb.product_id
            INNER JOIN stock s ON p.product_id = s.product_id
            WHERE lower(p.product_name) LIKE ?;
            """;

        List<Product> matches = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Construct the wildcard string dynamically inside the safety binder
            String searchPattern = "%" + productName.trim().toLowerCase() + "%";
            pstmt.setString(1, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // 1. Map Base Product State
                    Product product = new Product();
                    product.setProductId(rs.getInt("product_id"));
                    product.setProductName(rs.getString("product_name"));
                    product.setCostPrice(rs.getBigDecimal("cost_price"));
                    product.setRetailPrice(rs.getBigDecimal("retail_price"));
                    product.setCategoryId(rs.getShort("category"));
                    product.setCategoryName(rs.getString("category_name"));
                    product.setActive(rs.getBoolean("is_active"));

                    if (rs.getTimestamp("created_on") != null) {
                        product.setCreatedOn(rs.getTimestamp("created_on").toLocalDateTime());
                    }
                    if (rs.getTimestamp("updated_on") != null) {
                        product.setUpdatedOn(rs.getTimestamp("updated_on").toLocalDateTime());
                    }

                    // 2. Map Stock Tracking Sub-Entity

                    product.setCurrentQuantity(rs.getBigDecimal("current_stock"));
                    product.setMinStockLevel(rs.getBigDecimal("min_threshold"));
                    /*if (rs.getTimestamp("last_update") != null) {
                        stock.setLastUpdate(rs.getTimestamp("last_update").toLocalDateTime());
                    }*/
                    // Connect associations
                    matches.add(product);
                }
            }
        } catch (SQLException sqle) {
            System.err.println("CRITICAL: Segmented name query failed for query text: " + productName);
            sqle.printStackTrace();
        }

        return matches; // Safely returns the populated collection matrix or an empty list
    }

    @Override
    public Optional<Product> findByBarCode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();

        String sql = """
            SELECT *
            FROM product p
            JOIN category c on p.category = c.category_id
            INNER JOIN product_barcode pb ON p.product_id = pb.product_id
            INNER JOIN stock s ON p.product_id = s.product_id
            WHERE pb.barcode = ?;
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Build out the base product model properties
                    Product product = new Product();
                    product.setProductId(rs.getInt("product_id"));
                    product.setProductName(rs.getString("product_name"));
                    product.setCostPrice(rs.getBigDecimal("cost_price"));
                    product.setRetailPrice(rs.getBigDecimal("retail_price"));
                    product.setCategoryId(rs.getShort("category"));
                    product.setCategoryName(rs.getString("category_name"));
                    product.setActive(rs.getBoolean("is_active"));

                    // Handle optional timestamps safely
                    if (rs.getTimestamp("created_on") != null) {
                        product.setCreatedOn(rs.getTimestamp("created_on").toLocalDateTime());
                    }
                    if (rs.getTimestamp("updated_on") != null) {
                        product.setUpdatedOn(rs.getTimestamp("updated_on").toLocalDateTime());
                    }

                    // 2. Hydrate the composite Stock entity nested inside the product

                    product.setCurrentQuantity(rs.getBigDecimal("current_stock"));
                    product.setMinStockLevel(rs.getBigDecimal("min_threshold"));
                 /*   if (rs.getTimestamp("last_update") != null) {
                        stock.setLastUpdate(rs.getTimestamp("last_update").toLocalDateTime());
                    }*/

                    // Link them up
                   /* product.setStock(stock);
*/
                    return Optional.of(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to query barcode index registry for: " + barcode);
            e.printStackTrace();
        }

        return Optional.empty();
    }
}

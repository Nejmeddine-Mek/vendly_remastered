package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.math.BigDecimal;
import java.sql.*;
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
                PB.barcode AS barcode,
                PM.*
            FROM product P
            JOIN category C ON C.category_id = P.category
            LEFT JOIN product_barcode PB ON P.product_id = PB.product_id
            LEFT JOIN promotion PM ON P.product_id = PM.product_id;
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
                    Timestamp createdOn = rs.getTimestamp("created_on");
                    if (createdOn != null) product.setCreatedOn(createdOn.toLocalDateTime());

                    Timestamp updatedOn = rs.getTimestamp("updated_on");
                    if (updatedOn != null) product.setUpdatedOn(updatedOn.toLocalDateTime());

                    BigDecimal promoQuantity = rs.getBigDecimal("promo_quantity");
                    if (promoQuantity != null) product.setPromotionQuantity(promoQuantity);

                    BigDecimal promoPrice = rs.getBigDecimal("promo_price");
                    if (promoPrice != null) product.setPromotionPrice(promoPrice);

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
        if (processedProduct == null) return;

        if (processedProduct.getProductId() == 0) {
            insertProduct(processedProduct);
        } else {
            updateProduct(processedProduct);
        }
    }

    private void insertProduct(Product product) {
        // Notice product_id is omitted so SQLite's INTEGER PRIMARY KEY auto-increments
        String sql = """
        INSERT INTO product (product_name, cost_price, retail_price, category, is_active)
        VALUES (?, ?, ?, ?, ?);
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, product.getProductName());
            pstmt.setBigDecimal(2, product.getCostPrice());
            pstmt.setBigDecimal(3, product.getRetailPrice());
            pstmt.setShort(4, (short) product.getCategoryId());
            pstmt.setBoolean(5, product.isActive());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        product.setProductId(generatedId); // 🔑 Critical: Syncs new DB ID back to entity
                    }
                }
            }
        } catch (SQLException e) {
            if(e.getSQLState() != null && e.getSQLState().startsWith("23")){
                // this means the product is duplicated
                product.setProductId(-1);
                return;
            }
            System.err.println("Error inserting new product: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateProduct(Product product) {
        String sql = """
        UPDATE product 
        SET product_name = ?, 
            cost_price = ?, 
            retail_price = ?, 
            category = ?, 
            is_active = ?, 
            updated_on = CURRENT_TIMESTAMP
        WHERE product_id = ?;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, product.getProductName());
            pstmt.setBigDecimal(2, product.getCostPrice());
            pstmt.setBigDecimal(3, product.getRetailPrice());
            pstmt.setShort(4, (short) product.getCategoryId());
            pstmt.setBoolean(5, product.isActive());
            pstmt.setInt(6, product.getProductId());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating product ID " + product.getProductId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Product> findProduct(int productId) {
        return Optional.empty();
    }

    @Override
    public List<Product> findByName(String productName) {
        if (productName == null || productName.isBlank()) return new ArrayList<>();

        String sql = """
        SELECT DISTINCT 
            p.product_id, p.product_name, p.cost_price, p.retail_price, 
            p.category, p.is_active, p.created_on, p.updated_on,
            c.category_name,
            s.current_stock, s.min_threshold,
            pr.promo_quantity, pr.promo_price
        FROM product p
        INNER JOIN category c ON p.category = c.category_id
        INNER JOIN stock s ON p.product_id = s.product_id
        LEFT JOIN product_barcode pb ON p.product_id = pb.product_id
        LEFT JOIN promotion pr ON p.product_id = pr.product_id
        WHERE lower(p.product_name) LIKE ?;
        """;

        List<Product> matches = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + productName.trim().toLowerCase() + "%";
            pstmt.setString(1, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = mapResultSetToProduct(rs);
                    matches.add(product);
                }
            }
        } catch (SQLException sqle) {
            System.err.println("CRITICAL: Segmented name query failed for query text: " + productName);
            sqle.printStackTrace();
        }

        return matches;
    }

    @Override
    public Optional<Product> findByBarCode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();

        String sql = """
        SELECT 
            p.product_id, p.product_name, p.cost_price, p.retail_price, 
            p.category, p.is_active, p.created_on, p.updated_on,
            c.category_name,
            s.current_stock, s.min_threshold,
            pr.promo_quantity, pr.promo_price
        FROM product p
        INNER JOIN category c ON p.category = c.category_id
        INNER JOIN product_barcode pb ON p.product_id = pb.product_id
        INNER JOIN stock s ON p.product_id = s.product_id
        LEFT JOIN promotion pr ON p.product_id = pr.product_id
        WHERE pb.barcode = ?;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, barcode.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Product product = mapResultSetToProduct(rs);
                    return Optional.of(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("CRITICAL: Failed to query barcode index registry for: " + barcode);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public List<Product> getUncodedProducts() {
        String sql = """
                
                """;
        return List.of();
    }

    /**
     * Shared mapping helper to keep mapping DRY and eliminate duplicate JDBC calls.
     */
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setRetailPrice(rs.getBigDecimal("retail_price"));
        product.setCategoryId(rs.getShort("category"));
        product.setCategoryName(rs.getString("category_name"));
        product.setActive(rs.getBoolean("is_active"));

        // Optimized Timestamps mapping
        Timestamp createdOn = rs.getTimestamp("created_on");
        if (createdOn != null) {
            product.setCreatedOn(createdOn.toLocalDateTime());
        }

        Timestamp updatedOn = rs.getTimestamp("updated_on");
        if (updatedOn != null) {
            product.setUpdatedOn(updatedOn.toLocalDateTime());
        }

        // Stock attributes
        product.setCurrentQuantity(rs.getBigDecimal("current_stock"));
        product.setMinStockLevel(rs.getBigDecimal("min_threshold"));

        // Optimized Promotion attributes mapping
        BigDecimal promoQuantity = rs.getBigDecimal("promo_quantity");
        if (promoQuantity != null) {
            product.setPromotionQuantity(promoQuantity);
        }

        BigDecimal promoPrice = rs.getBigDecimal("promo_price");
        if (promoPrice != null) {
            product.setPromotionPrice(promoPrice);
        }

        return product;
    }
}

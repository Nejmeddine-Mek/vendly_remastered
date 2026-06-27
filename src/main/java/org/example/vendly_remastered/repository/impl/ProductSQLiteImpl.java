package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
}

package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Stock;
import org.example.vendly_remastered.repository.IStockRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StockSQLiteImpl implements IStockRepository {
    private Connection connection;

    public StockSQLiteImpl(Connection conn){ connection = conn; }
    @Override
    public void saveStock(int productId, Stock newStock) {
        String sql = """
                INSERT OR REPLACE stock (product_id, current_stock, min_threshold, last_update)
                VALUES (?,?,?,CURRENT_TIMESTAMP);
                """;
        try(PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setInt(1,productId);
            pstmt.setBigDecimal(2,newStock.getCurrentStock());
            pstmt.setBigDecimal(3,newStock.getMinThreshold());
            pstmt.executeUpdate();
        }catch (SQLException sqle){
            sqle.printStackTrace();
        }
    }

    @Override
    public Stock getStock(int productId) {
        String sql = "SELECT current_stock, min_threshold FROM stock WHERE product_id = ?;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Stock(
                            rs.getBigDecimal("current_stock"),
                            rs.getBigDecimal("min_threshold")
                    );
                }
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return null; // Safe fallback if product doesn't exist in stock table yet
    }

    @Override
    public Map<Integer, Stock> findAll() {
        String sql = "SELECT product_id, current_stock, min_threshold FROM stock;";
        Map<Integer, Stock> stockMap = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int productId = rs.getInt("product_id");
                Stock stock = new Stock(
                        rs.getBigDecimal("current_stock"),
                        rs.getBigDecimal("min_threshold")
                );
                stockMap.put(productId, stock);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return stockMap;
    }

}

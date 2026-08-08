package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.repository.ICategoryRepository;
import org.example.vendly_remastered.service.ErrorLogsService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorySQLiteImpl implements ICategoryRepository {
    Connection connection;
    public CategorySQLiteImpl(Connection conn){ this.connection = conn; }
    @Override
    public List<Category> findAll() {
        String sql = """
            SELECT category_id, category_name FROM category;
            """;
        List<Category> categories = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Category category = new Category();
                category.setCategoryId(rs.getShort("category_id"));
                category.setCategoryName(rs.getString("category_name"));
                categories.add(category);
            }

        } catch (SQLException sqle) {
            String message = "Database error occurred in CategorySQLiteImpl operation";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }

        return categories;
    }

    public int addCategory(String categoryName) {
        String sql = "INSERT INTO category (category_name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, categoryName);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1); // SQLite returns the last inserted row ID as the first column
                    }
                }
            }
        } catch (SQLException sqle) {
            String message = "Database error occurred in CategorySQLiteImpl operation";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, sqle);
        }
        return -1;
    }
}

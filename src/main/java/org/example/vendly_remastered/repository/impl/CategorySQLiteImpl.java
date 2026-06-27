package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.repository.ICategoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            sqle.printStackTrace();
        }

        return categories;
    }
}

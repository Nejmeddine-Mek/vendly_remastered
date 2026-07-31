package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Category;

import java.util.List;

public interface ICategoryRepository {
    public List<Category> findAll();
    public int addCategory(String categoryName);
}

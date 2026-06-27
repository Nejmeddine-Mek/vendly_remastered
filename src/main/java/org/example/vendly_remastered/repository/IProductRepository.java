package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Product;

import java.util.List;

public interface IProductRepository {
    public List<Product> findAll();
}

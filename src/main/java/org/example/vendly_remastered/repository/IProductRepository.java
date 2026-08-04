package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Product;

import java.util.List;
import java.util.Optional;

public interface IProductRepository {
    public List<Product> findAll();

    public void update(Product processedProduct);

    public void save(Product processedProduct);

    public Optional<Product> findProduct(int productId);
    public List<Product> findByName(String productName);
    public Optional<Product> findByBarCode(String barcode);

    public List<Product> getUncodedProducts();
}

package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Stock;

import java.math.BigDecimal;
import java.util.Map;

public interface IStockRepository {
    public void saveStock(int productId, Stock newStock);
    public Stock getStock(int productId);
    public Map<Integer, Stock> findAll();
}

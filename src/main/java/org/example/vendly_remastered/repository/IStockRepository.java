package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Stock;

import java.math.BigDecimal;
import java.util.Map;

public interface IStockRepository {
    public Map<Integer, Stock> findAll();
    public Stock getStock(int productId);
    public Map<Integer, BigDecimal> updateStockBatch(Map<Integer, Stock> stockUpdates);// only negative update
    public void addStock(int productId,Stock stock); // add new stock and create a batch corresponding to it
    public void addStockBulk(Map<Integer, Stock> itemsInInvoice, String deliveryNotes);
}

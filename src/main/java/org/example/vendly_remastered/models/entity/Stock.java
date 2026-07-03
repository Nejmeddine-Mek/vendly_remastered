package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.util.Objects;

public class Stock {
    private BigDecimal currentStock;
    private BigDecimal minThreshold;

    // Default constructor
    public Stock() {
        this.currentStock = BigDecimal.ZERO;
        this.minThreshold = BigDecimal.ZERO;
    }

    // Full constructor
    public Stock(BigDecimal currentStock, BigDecimal minThreshold) {
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
        this.minThreshold = minThreshold != null ? minThreshold : BigDecimal.ZERO;
    }

    // Getters and Setters
    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock != null ? currentStock : BigDecimal.ZERO;
    }

    public BigDecimal getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(BigDecimal minThreshold) {
        this.minThreshold = minThreshold != null ? minThreshold : BigDecimal.ZERO;
    }

    /**
     * Business logic helper to determine if stock has dipped below safe levels.
     */
    public boolean isLowStock() {
        return this.currentStock.compareTo(this.minThreshold) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(currentStock, stock.currentStock) &&
                Objects.equals(minThreshold, stock.minThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentStock, minThreshold);
    }

    @Override
    public String toString() {
        return "Stock{" +
                "currentStock=" + currentStock +
                ", minThreshold=" + minThreshold +
                '}';
    }
}
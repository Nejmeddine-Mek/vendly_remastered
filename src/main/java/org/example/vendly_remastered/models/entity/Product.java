package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private int productId;
    private String productName;
    private BigDecimal costPrice;
    private BigDecimal retailPrice;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private short categoryId;
    private boolean isActive;

    // Transient Display Property (Not a column in the product table)
    private String categoryName;
    private String barcode;
    private List<String> barcodes = new ArrayList<>();
    private final Stock currentStock;
    private final Promotion promotion;

    public Product() {
        this.currentStock = new Stock();
        this.promotion = new Promotion();
        this.barcodes = new ArrayList<>();
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }

    public LocalDateTime getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(LocalDateTime updatedOn) { this.updatedOn = updatedOn; }

    public short getCategoryId() { return categoryId; }
    public void setCategoryId(short categoryId) { this.categoryId = categoryId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public List<String> getBarcodes() { return barcodes; }
    public void setBarcodes(List<String> barcodes) { this.barcodes = barcodes; }

    public BigDecimal getCurrentQuantity() {
        return currentStock.getCurrentStock();
    }
    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentStock.setCurrentStock(currentQuantity != null ? currentQuantity : BigDecimal.ZERO);
    }

    public BigDecimal getMinStockLevel() {
        return currentStock.getMinThreshold();
    }
    public void setMinStockLevel(BigDecimal minStockLevel) {
        this.currentStock.setMinThreshold(minStockLevel != null ? minStockLevel : BigDecimal.ZERO);
    }

    public Stock getCurrentStock() {
        return currentStock;
    }
    public boolean hasPromotion(){ return promotion.getPrice() != null; }
    public BigDecimal getPromotionPrice(){ return promotion.getPrice(); }
    public void setPromotionPrice(BigDecimal newP){ promotion.setPrice(newP);}

    public BigDecimal getPromotionQuantity(){ return promotion.getQuantity(); }
    public void setPromotionQuantity(BigDecimal newQ){ promotion.setQuantity(newQ);}
}

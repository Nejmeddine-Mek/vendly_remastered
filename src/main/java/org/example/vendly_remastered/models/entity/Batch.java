package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Batch {
    private int batchId;
    private int productId;
    private String productName;
    private String batchNumber;
    private BigDecimal initialReceivedQty;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;

    // Default Constructor
    public Batch() {}

    // Convenience Constructor for creating new incoming inventory entries
    public Batch(int productId,String productName ,String batchNumber, BigDecimal initialReceivedQty, LocalDate expiryDate) {
        this.productId = productId;
        this.productName = productName;
        this.batchNumber = batchNumber == null ? "GENERIC" : batchNumber;
        this.initialReceivedQty = initialReceivedQty;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public BigDecimal getInitialReceivedQty() { return initialReceivedQty; }
    public void setInitialReceivedQty(BigDecimal initialReceivedQty) { this.initialReceivedQty = initialReceivedQty; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}
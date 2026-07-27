package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchaseDetailItem {
    private Long purchaseId;
    private int productId;
    private Integer itemNumber; // Line number order in table (1, 2, 3...)
    private String productName; // Display field for TableView
    private BigDecimal unitCost;
    private BigDecimal quantity;
    private BigDecimal subtotal;

    private String batchNumber;
    private LocalDate expiryDate;

    // Default Constructor
    public PurchaseDetailItem() {
        this.itemNumber = 1;
        this.unitCost = BigDecimal.ZERO;
        this.quantity = BigDecimal.ONE;
        this.subtotal = BigDecimal.ZERO;
    }

    // Constructor without purchaseId (used when building entry rows before saving)
    public PurchaseDetailItem(Integer itemNumber, int productId, String productName,
                              BigDecimal unitCost, BigDecimal quantity) {
        this.itemNumber = itemNumber;
        this.productId = productId;
        this.productName = productName;
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.subtotal = calculateSubtotal();
    }

    // Full Parameterized Constructor
    public PurchaseDetailItem(Long purchaseId, int productId, Integer itemNumber,
                              String productName, BigDecimal unitCost, BigDecimal quantity) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.itemNumber = itemNumber;
        this.productName = productName;
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.subtotal = calculateSubtotal();
    }

    // --- HELPER METHODS ---

    /**
     * Calculates the subtotal for this item line (unitCost * quantity).
     */
    public BigDecimal calculateSubtotal() {
        if (unitCost == null || quantity == null) return BigDecimal.ZERO;
        return unitCost.multiply(quantity);
    }

    // --- GETTERS & SETTERS ---

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Integer getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(Integer itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
        this.subtotal = calculateSubtotal();
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        this.subtotal = calculateSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal != null ? subtotal : calculateSubtotal();
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public String toString() {
        return "PurchaseDetailItem{" +
                "purchaseId=" + purchaseId +
                ", productId=" + productId +
                ", itemNumber=" + itemNumber +
                ", productName='" + productName + '\'' +
                ", unitCost=" + unitCost +
                ", quantity=" + quantity +
                ", subtotal=" + subtotal +
                '}';
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;

public class SaleDetailItem {
    private int saleId;
    private int productId;
    private String productName;
    private BigDecimal productQuantity;
    private BigDecimal productUnitPrice;

    // Default No-Arg Constructor (Essential for serialization and mapping libraries)
    public SaleDetailItem() {
    }

    // Full Parameterized Constructor
    public SaleDetailItem(int saleId, int productId, String productName, BigDecimal productQuantity, BigDecimal productUnitPrice) {
        this.saleId = saleId;
        this.productId = productId;
        this.productName = productName;
        this.productQuantity = productQuantity;
        this.productUnitPrice = productUnitPrice;
    }

    // --- Getters and Setters ---

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getProductQuantity() {
        return productQuantity;
    }

    public void setProductQuantity(BigDecimal productQuantity) {
        this.productQuantity = productQuantity;
    }

    public BigDecimal getProductUnitPrice() {
        return productUnitPrice;
    }

    public void setProductUnitPrice(BigDecimal productUnitPrice) {
        this.productUnitPrice = productUnitPrice;
    }

    // --- Helper Domain Logic ---

    /**
     * Helper calculation utility to instantly compute line-item totals
     * without cluttering controller classes.
     */
    public BigDecimal getSubTotal() {
        if (productQuantity == null || productUnitPrice == null) {
            return BigDecimal.ZERO;
        }
        return productQuantity.multiply(productUnitPrice);
    }

    @Override
    public String toString() {
        return "SaleDetailItem{" +
                "saleId=" + saleId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", productQuantity=" + productQuantity +
                ", productUnitPrice=" + productUnitPrice +
                ", subTotal=" + getSubTotal() +
                '}';
    }
}

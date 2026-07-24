package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseHeader {
    private Long purchaseId;
    private Long supplierId;
    private Long userId;
    private BigDecimal purchaseCost;
    private BigDecimal amountPaid;
    private LocalDateTime purchaseDate;

    // Display / Joined Fields (for TableView & UI Inspection)
    private String supplierName;
    private String userName;

    // Default Constructor
    public PurchaseHeader() {
        this.purchaseCost = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
        this.purchaseDate = LocalDateTime.now();
    }

    // Full Parameterized Constructor
    public PurchaseHeader(Long purchaseId, Long supplierId, Long userId,
                          BigDecimal purchaseCost, BigDecimal amountPaid, LocalDateTime purchaseDate) {
        this.purchaseId = purchaseId;
        this.supplierId = supplierId;
        this.userId = userId;
        this.purchaseCost = purchaseCost != null ? purchaseCost : BigDecimal.ZERO;
        this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDateTime.now();
    }

    // --- HELPER METHODS ---

    /**
     * Calculates the remaining unpaid balance (Debt) for this purchase.
     * @return purchaseCost - amountPaid (Clamped to zero if overpaid)
     */
    public BigDecimal getUnpaidBalance() {
        if (purchaseCost == null) return BigDecimal.ZERO;
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal diff = purchaseCost.subtract(paid);
        return diff.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : diff;
    }

    // --- GETTERS & SETTERS ---

    public Long getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "PurchaseHeader{" +
                "purchaseId=" + purchaseId +
                ", supplierId=" + supplierId +
                ", userId=" + userId +
                ", purchaseCost=" + purchaseCost +
                ", amountPaid=" + amountPaid +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}

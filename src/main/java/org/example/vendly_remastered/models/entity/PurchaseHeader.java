package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PurchaseHeader {
    private Long purchaseId;
    private Long supplierId;
    private Long userId;

    // Document & Reference Numbers
    private String invoiceNumber;

    // Fiscal / Legal Numbers (Snapshot from Supplier)
    private String rcNumber;
    private String nifNumber;
    private String nisNumber;
    private String artNumber;
    private String cpaAccount;

    // Financial & Tax Breakdown
    private BigDecimal totalHt;
    private BigDecimal discountAmount; // Remise
    private BigDecimal tvaRate;        // e.g. 19.00
    private BigDecimal tvaAmount;
    private BigDecimal stampDuty;      // Timbre

    // Final Settlement
    private BigDecimal purchaseCost;   // Net to Pay (TTC)
    private BigDecimal amountPaid;
    private LocalDateTime purchaseDate;

    // Display / Joined Fields (for TableView & UI Inspection)
    private String supplierName;
    private String userName;

    // Default Constructor
    public PurchaseHeader() {
        this.totalHt = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.tvaRate = new BigDecimal("19.00");
        this.tvaAmount = BigDecimal.ZERO;
        this.stampDuty = BigDecimal.ZERO;
        this.purchaseCost = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
        this.purchaseDate = LocalDateTime.now();
    }

    // Full Parameterized Constructor
    public PurchaseHeader(Long purchaseId, Long supplierId, Long userId, String invoiceNumber,
                          String rcNumber, String nifNumber, String nisNumber, String artNumber, String cpaAccount,
                          BigDecimal totalHt, BigDecimal discountAmount, BigDecimal tvaRate, BigDecimal tvaAmount,
                          BigDecimal stampDuty, BigDecimal purchaseCost, BigDecimal amountPaid, LocalDateTime purchaseDate) {
        this.purchaseId = purchaseId;
        this.supplierId = supplierId;
        this.userId = userId;
        this.invoiceNumber = invoiceNumber;
        this.rcNumber = rcNumber;
        this.nifNumber = nifNumber;
        this.nisNumber = nisNumber;
        this.artNumber = artNumber;
        this.cpaAccount = cpaAccount;
        this.totalHt = totalHt != null ? totalHt : BigDecimal.ZERO;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.tvaRate = tvaRate != null ? tvaRate : new BigDecimal("19.00");
        this.tvaAmount = tvaAmount != null ? tvaAmount : BigDecimal.ZERO;
        this.stampDuty = stampDuty != null ? stampDuty : BigDecimal.ZERO;
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getRcNumber() {
        return rcNumber;
    }

    public void setRcNumber(String rcNumber) {
        this.rcNumber = rcNumber;
    }

    public String getNifNumber() {
        return nifNumber;
    }

    public void setNifNumber(String nifNumber) {
        this.nifNumber = nifNumber;
    }

    public String getNisNumber() {
        return nisNumber;
    }

    public void setNisNumber(String nisNumber) {
        this.nisNumber = nisNumber;
    }

    public String getArtNumber() {
        return artNumber;
    }

    public void setArtNumber(String artNumber) {
        this.artNumber = artNumber;
    }

    public String getCpaAccount() {
        return cpaAccount;
    }

    public void setCpaAccount(String cpaAccount) {
        this.cpaAccount = cpaAccount;
    }

    public BigDecimal getTotalHt() {
        return totalHt;
    }

    public void setTotalHt(BigDecimal totalHt) {
        this.totalHt = totalHt;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTvaRate() {
        return tvaRate;
    }

    public void setTvaRate(BigDecimal tvaRate) {
        this.tvaRate = tvaRate;
    }

    public BigDecimal getTvaAmount() {
        return tvaAmount;
    }

    public void setTvaAmount(BigDecimal tvaAmount) {
        this.tvaAmount = tvaAmount;
    }

    public BigDecimal getStampDuty() {
        return stampDuty;
    }

    public void setStampDuty(BigDecimal stampDuty) {
        this.stampDuty = stampDuty;
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
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", totalHt=" + totalHt +
                ", discountAmount=" + discountAmount +
                ", tvaRate=" + tvaRate +
                ", tvaAmount=" + tvaAmount +
                ", stampDuty=" + stampDuty +
                ", purchaseCost=" + purchaseCost +
                ", amountPaid=" + amountPaid +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}
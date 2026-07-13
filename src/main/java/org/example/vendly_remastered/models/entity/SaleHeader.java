package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleHeader {
    private int saleId;
    private LocalDateTime saleDate;
    private BigDecimal saleTotal;
    private BigDecimal paidAmount;
    private BigDecimal saleProfit;
    private int saleUser;
    private String saleUsername; // Transient property for easy UI rendering
    private Integer saleClient;  // Can be null for cash/anonymous system sales
    private String clientName;   // Transient property for UI TableView mapping
    private String saleNote;     // Restrained to "sale", "return", "expired"

    // Default Constructor
    public SaleHeader() {
        this.saleId = 0;
        this.saleDate = LocalDateTime.now();
        this.saleTotal = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.saleProfit = BigDecimal.ZERO;
        this.saleNote = "sale"; // Default fallback status
    }

    // Full Primary Constructor
    public SaleHeader(int saleId, LocalDateTime saleDate, BigDecimal saleTotal, BigDecimal paidAmount,
                      BigDecimal saleProfit, int saleUser, String saleUsername, Integer saleClient,
                      String clientName, String saleNote) {
        this.saleId = saleId;
        this.saleDate = saleDate;
        this.saleTotal = saleTotal != null ? saleTotal : BigDecimal.ZERO;
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.saleProfit = saleProfit != null ? saleProfit : BigDecimal.ZERO;
        this.saleUser = saleUser;
        this.saleUsername = saleUsername;
        this.saleClient = saleClient;
        this.clientName = clientName;
        this.saleNote = saleNote;
    }

    // High-Utility Business Logic Helper Methods

    /**
     * Calculates the remaining debt on this transaction instantly.
     * Uses subtract() to maintain exact scale precision.
     */
    public BigDecimal getRemainingDebt() {
        return this.saleTotal.subtract(this.paidAmount);
    }

    /**
     * Quick check to see if this transaction was completely paid off.
     * compareTo() == 0 checks value equality regardless of scale (e.g., 0.0 vs 0.00).
     */
    public boolean isFullyPaid() {
        return getRemainingDebt().compareTo(BigDecimal.ZERO) <= 0;
    }

    // Getters and Setters
    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }

    public BigDecimal getSaleTotal() { return saleTotal; }
    public void setSaleTotal(BigDecimal saleTotal) { this.saleTotal = saleTotal; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getSaleProfit() { return saleProfit; }
    public void setSaleProfit(BigDecimal saleProfit) { this.saleProfit = saleProfit; }

    public int getSaleUser() { return saleUser; }
    public void setSaleUser(int saleUser) { this.saleUser = saleUser; }

    public String getSaleUsername() { return saleUsername; }
    public void setSaleUsername(String saleUsername) { this.saleUsername = saleUsername; }

    public Integer getSaleClient() { return saleClient; }
    public void setSaleClient(Integer saleClient) { this.saleClient = saleClient; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getSaleNote() { return saleNote; }
    public void setSaleNote(String saleNote) { this.saleNote = saleNote; }
    @Override
    public String toString() {
        String formattedDate = (saleDate != null)
                ? saleDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "N/A";

        return "SaleHeader{" +
                "saleId=" + saleId +
                ", date=" + formattedDate +
                ", total=" + (saleTotal != null ? saleTotal.toPlainString() + " DA" : "0.00 DA") +
                ", paid=" + (paidAmount != null ? paidAmount.toPlainString() + " DA" : "0.00 DA") +
                ", profit=" + (saleProfit != null ? saleProfit.toPlainString() + " DA" : "0.00 DA") +
                ", user=[" + saleUser + (saleUsername != null ? " - " + saleUsername : "") + "]" +
                ", client=" + (saleClient != null ? "[" + saleClient + (clientName != null ? " - " + clientName : "") + "]" : "ANONYMOUS") +
                ", status='" + saleNote + '\'' +
                '}';
    }
}
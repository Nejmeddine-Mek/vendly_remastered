package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleHeader {

    private long saleId;
    private LocalDateTime saleDate;
    private BigDecimal saleTotal;
    private BigDecimal saleProfit;
    private int saleUserId;
    private String saleUserName;
    private int saleClient;
    private String saleClientName;

    public SaleHeader(long id, LocalDateTime saleDate,BigDecimal saleTotal, BigDecimal saleProfit,
                        int userId, String userName, int saleClient, String clientName){
        this.saleId = id;
        this.saleDate = saleDate;
        this.saleTotal = saleTotal;
        this.saleProfit = saleProfit;
        this.saleUserId = userId;
        this.saleUserName = userName;
        this.saleClient = saleClient;
        this.saleClientName = clientName;
    }

    public long getSaleId() {
        return saleId;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public BigDecimal getSaleTotal() {
        return saleTotal;
    }

    public BigDecimal getSaleProfit() {
        return saleProfit;
    }

    public int getSaleUserId() {
        return saleUserId;
    }

    public String getSaleUserName() {
        return saleUserName;
    }

    public int getSaleClient() {
        return saleClient;
    }

    public String getSaleClientName() {
        return saleClientName;
    }
}

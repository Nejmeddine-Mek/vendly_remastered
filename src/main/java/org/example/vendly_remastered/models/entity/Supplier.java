package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Supplier {

    private Long supplierId;
    private String supplierCompany;
    private String supplierFullName;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer createdBy;
    private String createdByUsername;
    private String email;
    private BigDecimal debts;
    private boolean isActive;
    // Default Constructor
    public Supplier() {
        this.debts = new BigDecimal(0);
    }

    // Full Constructor
    public Supplier(Long supplierId, String supplierCompany, String supplierFullName,
                    String phoneNumber, LocalDateTime createdAt, LocalDateTime updatedAt,
                    Integer createdBy, String createdByUsername, BigDecimal debts) {
        this.supplierId = supplierId;
        this.supplierCompany = supplierCompany;
        this.supplierFullName = supplierFullName;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.createdByUsername = createdByUsername;
        this.debts = debts != null ? debts : BigDecimal.ZERO;
    }

    // Getters and Setters

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierCompany() {
        return supplierCompany;
    }

    public void setSupplierCompany(String supplierCompany) {
        this.supplierCompany = supplierCompany;
    }

    public String getSupplierFullName() {
        return supplierFullName;
    }

    public void setSupplierFullName(String supplierFullName) {
        this.supplierFullName = supplierFullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public BigDecimal getDebts() {
        return debts;
    }

    public void setDebts(BigDecimal debts) {
        this.debts = debts;
    }

    @Override
    public String toString() {
        return supplierCompany != null && !supplierCompany.isBlank()
                ? supplierCompany
                : supplierFullName;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    public boolean isActive(){return this.isActive; }
}

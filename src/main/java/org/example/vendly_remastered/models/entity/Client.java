package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Client {
    private int clientId;
    private String companyName;       // Can be null/empty for individuals
    private String contactFullName;   // The main person's name
    private String email;
    private String phone;
    private String address;
    private BigDecimal currentDebts;
    private boolean isActive;
    private int createdBy;
    private String createdByUsername; // For easy UI TableView cell mapping
    private LocalDateTime createdAt;

    // Default Constructor (Useful for clean multi-form bindings)
    public Client(boolean isActive) {
        this.isActive = isActive;
        this.clientId = 0; // 0 indicates a new, unpersisted client profile
        this.currentDebts = new BigDecimal(0);
        this.createdAt = LocalDateTime.now();
    }

    // Full Primary Constructor
    public Client(int clientId, String companyName, String contactFullName, String email,
                  String phone, String address, boolean isActive, int createdBy, String createdByUsername, LocalDateTime createdAt) {
        this.clientId = clientId;
        this.companyName = companyName;
        this.contactFullName = contactFullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.isActive = isActive;
        this.createdBy = createdBy;
        this.createdByUsername = createdByUsername;
        this.createdAt = createdAt;
    }

    public Client() {
        this.currentDebts = new BigDecimal(0);
    }

    // Getters and Setters
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactFullName() { return contactFullName; }
    public void setContactFullName(String contactFullName) { this.contactFullName = contactFullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return isActive;
    }
    // 3. The standard setter
    public void setActive(boolean active) {
        this.isActive = active;
    }

    public BigDecimal getCurrentDebts() {
        return currentDebts;
    }

    public void setCurrentDebts(BigDecimal currentDebts) {
        this.currentDebts = currentDebts;
        if(currentDebts.compareTo(BigDecimal.ZERO) < 0)
            this.currentDebts = BigDecimal.ZERO;
    }
}

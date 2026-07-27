package org.example.vendly_remastered.security;

public class AppConfig {

    private static AppConfig instance;

    // --- Section 1: Localization ---
    private String language = "English";

    // --- Section 2: Inventory & Stock Policies ---
    private boolean trackStock = true;
    private boolean updateStockFromPurchases = true;
    private boolean allowNegativeStock = false;
    private boolean enableStockAlerts = true;
    private boolean enableExpiryAlerts = true;

    // --- Section 3: Debts & Account Balances ---
    private boolean trackCustomerDebts = true;
    private boolean warnOverdueDebt = true;
    private boolean trackSupplierDebts = true;

    // Private constructor for Singleton pattern
    private AppConfig() {}

    public static AppConfig getInstance() {
        if (instance == null) {

        }
        return instance;
    }

    public static void init() {

    }

    // --- Getters & Setters ---

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isTrackStock() { return trackStock; }
    public void setTrackStock(boolean trackStock) { this.trackStock = trackStock; }

    public boolean isUpdateStockFromPurchases() { return updateStockFromPurchases; }
    public void setUpdateStockFromPurchases(boolean updateStockFromPurchases) { this.updateStockFromPurchases = updateStockFromPurchases; }

    public boolean isAllowNegativeStock() { return allowNegativeStock; }
    public void setAllowNegativeStock(boolean allowNegativeStock) { this.allowNegativeStock = allowNegativeStock; }

    public boolean isEnableStockAlerts() { return enableStockAlerts; }
    public void setEnableStockAlerts(boolean enableStockAlerts) { this.enableStockAlerts = enableStockAlerts; }

    public boolean isEnableExpiryAlerts() { return enableExpiryAlerts; }
    public void setEnableExpiryAlerts(boolean enableExpiryAlerts) { this.enableExpiryAlerts = enableExpiryAlerts; }

    public boolean isTrackCustomerDebts() { return trackCustomerDebts; }
    public void setTrackCustomerDebts(boolean trackCustomerDebts) { this.trackCustomerDebts = trackCustomerDebts; }

    public boolean isWarnOverdueDebt() { return warnOverdueDebt; }
    public void setWarnOverdueDebt(boolean warnOverdueDebt) { this.warnOverdueDebt = warnOverdueDebt; }

    public boolean isTrackSupplierDebts() { return trackSupplierDebts; }
    public void setTrackSupplierDebts(boolean trackSupplierDebts) { this.trackSupplierDebts = trackSupplierDebts; }
}
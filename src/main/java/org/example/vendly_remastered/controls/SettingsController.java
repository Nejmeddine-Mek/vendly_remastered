package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.vendly_remastered.security.AppConfig;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    // --- Section 1: Localization & Region ---
    @FXML private ComboBox<String> languageComboBox;

    // --- Section 2: Inventory & Stock Policies ---
    @FXML private CheckBox trackStockCheckBox;
    @FXML private CheckBox updateStockFromPurchasesCheckBox;
    @FXML private CheckBox allowNegativeStockCheckBox;
    @FXML private CheckBox enableStockAlertsCheckBox;
    @FXML private CheckBox enableExpiryAlertsCheckBox;

    // --- Section 3: Debts & Account Balances ---
    @FXML private CheckBox trackCustomerDebtsCheckBox;
    @FXML private CheckBox warnOverdueDebtCheckBox;
    @FXML private CheckBox trackSupplierDebtsCheckBox;

    // --- Section 4: Product Categories ---
    @FXML private TextField txtNewCategoryName;
    @FXML private Button btnAddCategory;
    @FXML private ListView<String> categoryListView;

    // --- Section 5: Roles & Security Permissions ---
    @FXML private ComboBox<String> rolesComboBox;
    @FXML private Button btnNewRole;
    @FXML private VBox permissionsPanel;
    @FXML private CheckBox permAccessPOS;
    @FXML private CheckBox permManagePurchases;
    @FXML private CheckBox permManageProducts;
    @FXML private CheckBox permViewReports;
    @FXML private CheckBox permManageUsers;
    @FXML private CheckBox permApplyDiscounts;

    // --- Bottom Action Bar ---
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // 1. Populate Language Dropdown
        languageComboBox.setItems(FXCollections.observableArrayList("English", "French", "Arabic"));

        // 2. Load configurations from AppConfig singleton instance
        loadConfigValues();
    }
    private void loadConfigValues() {
        AppConfig config = AppConfig.getInstance();
        if (config == null) {
            return; // Guard until getInstance() implementation is ready
        }

        // Language
        if (config.getLanguage() != null) {
            languageComboBox.setValue(config.getLanguage());
        }

        // Inventory Policies
        trackStockCheckBox.setSelected(config.isTrackStock());
        updateStockFromPurchasesCheckBox.setSelected(config.isUpdateStockFromPurchases());
        allowNegativeStockCheckBox.setSelected(config.isAllowNegativeStock());
        enableStockAlertsCheckBox.setSelected(config.isEnableStockAlerts());
        enableExpiryAlertsCheckBox.setSelected(config.isEnableExpiryAlerts());

        // Debt & Balance Policies
        trackCustomerDebtsCheckBox.setSelected(config.isTrackCustomerDebts());
        warnOverdueDebtCheckBox.setSelected(config.isWarnOverdueDebt());
        trackSupplierDebtsCheckBox.setSelected(config.isTrackSupplierDebts());
    }

    @FXML
    private void handleSaveCategory(ActionEvent event) {
        // Pending category repository integration
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        AppConfig config = AppConfig.getInstance();
        if (config == null) {
            return;
        }

        // Persist UI states back into AppConfig
        config.setLanguage(languageComboBox.getValue());

        config.setTrackStock(trackStockCheckBox.isSelected());
        config.setUpdateStockFromPurchases(updateStockFromPurchasesCheckBox.isSelected());
        config.setAllowNegativeStock(allowNegativeStockCheckBox.isSelected());
        config.setEnableStockAlerts(enableStockAlertsCheckBox.isSelected());
        config.setEnableExpiryAlerts(enableExpiryAlertsCheckBox.isSelected());

        config.setTrackCustomerDebts(trackCustomerDebtsCheckBox.isSelected());
        config.setWarnOverdueDebt(warnOverdueDebtCheckBox.isSelected());
        config.setTrackSupplierDebts(trackSupplierDebtsCheckBox.isSelected());
    }



}
package org.example.vendly_remastered.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.models.entity.Role;
import org.example.vendly_remastered.repository.ICategoryRepository;
import org.example.vendly_remastered.repository.IRoleRepository;
import org.example.vendly_remastered.security.AppConfig;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.security.UserSession;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static javafx.beans.binding.Bindings.createBooleanBinding;

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
    @FXML private ListView<Role> roleListView;
    @FXML private TextField txtNewRoleName;
    @FXML private Button btnAddRole;
    @FXML private Button btnNewRole;

    @FXML private VBox rolesPanel;
    @FXML private VBox categoryPanel;
    @FXML private CheckBox permCanSell;
    @FXML private CheckBox permCanSeeClients;
    @FXML private CheckBox permCanEditClients;
    @FXML private CheckBox permCanSeeProducts;
    @FXML private CheckBox permCanEditProducts;
    @FXML private CheckBox permCanSeeSuppliers;
    @FXML private CheckBox permCanEditSuppliers;
    @FXML private CheckBox permCanSeeHistory;
    @FXML private CheckBox permCanSeeDashboard;
    @FXML private CheckBox permCanSeeSettings;
    @FXML private CheckBox permCanSeeUsers;
    @FXML private CheckBox permCanSeePurchases;
    @FXML private CheckBox permCanEditPurchases;

    // --- Bottom Action Bar ---
    @FXML private Button btnSavePermissions;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private boolean isUpdatingStates = false;
    private final BooleanProperty[] changedPermissions = new BooleanProperty[13];
    private final boolean[] originalPermissions = new boolean[13];
    private final BooleanProperty hasChanged = new SimpleBooleanProperty(false);
    private IRoleRepository roleRepository;
    private ICategoryRepository categoryRepository;

    public void setRoleRepository(IRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void setCategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializePermissionsBoxes();
        hasChanged.bind(
                createBooleanBinding(() -> {
                    for (int i = 0; i < changedPermissions.length; i++) {
                        if (changedPermissions[i].get() != originalPermissions[i]) {
                            return true; // The moment one differs, the whole thing becomes true
                        }
                    }
                    return false;
                }, changedPermissions) // Pass the array as dependencies so it re-evaluates automatically!
        );
        btnSavePermissions.managedProperty().bind(btnSavePermissions.visibleProperty());
        btnSavePermissions.visibleProperty().bind(hasChanged);

        if(!UserSession.getInstance().hasPrivilege("can_edit_products")){
            categoryPanel.managedProperty().bind(categoryPanel.visibleProperty());
            categoryPanel.setVisible(false);
        }
        if(!UserSession.getInstance().hasPrivilege("can_see_users")){
            rolesPanel.managedProperty().bind(rolesPanel.visibleProperty());
            rolesPanel.setVisible(false);
        }
        // 1. Populate Language Dropdown
        languageComboBox.setItems(FXCollections.observableArrayList("English", "Français", "العربية"));
        setRolesListBehaviour();

        // 2. Load configurations from AppConfig singleton instance
        loadConfigValues();

        // 3. set button behaviour
        btnAddCategory.setOnAction(_ -> handleAddCategory() );
        btnAddRole.setOnAction( _ -> handleAddRole());
        btnSavePermissions.setOnAction(_ -> handleSavePermissions());
        saveButton.setOnAction(this::handleSaveSettings);

    }

    private void loadConfigValues() {
        // hide this for now
        warnOverdueDebtCheckBox.setManaged(false);
        warnOverdueDebtCheckBox.setVisible(false);

        Config config = Config.getInstance();
        String language = config.getLanguage(); // e.g., "en", "fr", "ar"

        if ("ar".equals(language)) {
            languageComboBox.setValue("العربية");
        } else if ("fr".equals(language)) {
            languageComboBox.setValue("Français");
        } else {
            languageComboBox.setValue("English"); // default fallback
        }
        trackStockCheckBox.setSelected(config.isTrackStock());
        updateStockFromPurchasesCheckBox.setSelected(config.isUpdateStockOnSale());
        allowNegativeStockCheckBox.setSelected(config.isAllowNegativeStock());

        enableStockAlertsCheckBox.setSelected(config.isLowStockPOSAlert());
        enableExpiryAlertsCheckBox.setSelected(config.isBatchExpiryAlerts());

        trackCustomerDebtsCheckBox.setSelected(config.isTrackClientsDebts());
        trackSupplierDebtsCheckBox.setSelected(config.isTrackSupplierDebts());
    }

    private void initializePermissionsBoxes(){
        CheckBox[] permissionCheckboxes = {
                permCanSell, permCanSeeClients, permCanEditClients,
                permCanSeeProducts, permCanEditProducts, permCanSeeSuppliers,
                permCanEditSuppliers, permCanSeeHistory, permCanSeeDashboard,
                permCanSeeSettings, permCanSeeUsers, permCanSeePurchases,
                permCanEditPurchases
        };
        for(int i = 0; i < permissionCheckboxes.length; ++i){
            originalPermissions[i] = permissionCheckboxes[i].isSelected();
            changedPermissions[i] = new SimpleBooleanProperty(originalPermissions[i]);
        }

        for(int i =  0; i < permissionCheckboxes.length; ++i){
            final int index = i;
            permissionCheckboxes[i].selectedProperty().addListener((_, _, _) -> {
                if(!isUpdatingStates)
                    this.changedPermissions[index].setValue(!this.changedPermissions[index].getValue());
            });
        }
    }

    @FXML
    private void handleSaveSettings(ActionEvent event) {
        Config config = Config.getInstance();
        if (config == null) {
            return;
        }

        // Persist UI states back into AppConfig
        config.setLanguage(languageComboBox.getValue());
        config.setTrackStock(trackStockCheckBox.isSelected());
        config.setUpdateStockOnSale(updateStockFromPurchasesCheckBox.isSelected());
        config.setAllowNegativeStock(allowNegativeStockCheckBox.isSelected());
        config.setLowStockPOSAlert(enableStockAlertsCheckBox.isSelected());
        config.setBatchExpiryAlerts(enableExpiryAlertsCheckBox.isSelected());

        config.setTrackClientsDebts(trackCustomerDebtsCheckBox.isSelected());
        config.setTrackSupplierDebts(warnOverdueDebtCheckBox.isSelected());
        config.setTrackSupplierDebts(trackSupplierDebtsCheckBox.isSelected());

        config.saveConfigToFile();
    }


    private void loadCategories(){
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            if (category.getCategoryName() != null && !category.getCategoryName().trim().isEmpty()) {
                categoryListView.getItems().add(category.getCategoryName());
            }
        }
    }
    private void loadRoles(){

        List<Role> rolesList = roleRepository.findAll();
        for(Role role : rolesList){
            roleListView.getItems().add(role);
        }
    }


    private void setRolesListBehaviour(){
        roleListView.setCellFactory(param -> new ListCell<Role>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null || role.getRoleName() == null) {
                    setText(null);
                } else {
                    setText(role.getRoleName());
                }
            }
        });

        roleListView.getSelectionModel().selectedItemProperty().addListener(_ -> {
            Role selectedRole = roleListView.getSelectionModel().getSelectedItem();
            if(selectedRole == null)
                return;
            isUpdatingStates = true;
            permCanSell.setSelected(selectedRole.hasPermission("can_sell"));
            permCanSeeClients.setSelected(selectedRole.hasPermission("can_see_clients"));
            permCanEditClients.setSelected(selectedRole.hasPermission("can_edit_clients"));
            permCanSeeProducts.setSelected(selectedRole.hasPermission("can_see_products"));
            permCanEditProducts.setSelected(selectedRole.hasPermission("can_edit_products"));
            permCanSeeSuppliers.setSelected(selectedRole.hasPermission("can_see_suppliers"));
            permCanEditSuppliers.setSelected(selectedRole.hasPermission("can_edit_suppliers"));
            permCanSeeHistory.setSelected(selectedRole.hasPermission("can_see_history"));
            permCanSeeDashboard.setSelected(selectedRole.hasPermission("can_see_dashboard"));
            permCanSeeSettings.setSelected(selectedRole.hasPermission("can_see_settings"));
            permCanSeeUsers.setSelected(selectedRole.hasPermission("can_see_users"));
            permCanSeePurchases.setSelected(selectedRole.hasPermission("can_see_purchases"));
            permCanEditPurchases.setSelected(selectedRole.hasPermission("can_edit_purchases"));
            isUpdatingStates = false;
        });
    }

    private void handleSavePermissions(){
        Role selectedRole = roleListView.getSelectionModel().getSelectedItem();
        if(selectedRole == null)
            return;
        selectedRole.setPermission("can_sell",permCanSell.isSelected());
        selectedRole.setPermission("can_see_clients",permCanSeeClients.isSelected());
        selectedRole.setPermission("can_edit_clients",permCanEditClients.isSelected());
        selectedRole.setPermission("can_see_products",permCanSeeProducts.isSelected());
        selectedRole.setPermission("can_edit_products",permCanEditProducts.isSelected());
        selectedRole.setPermission("can_see_suppliers",permCanSeeSuppliers.isSelected());
        selectedRole.setPermission("can_edit_suppliers",permCanEditSuppliers.isSelected());
        selectedRole.setPermission("can_see_history",permCanSeeHistory.isSelected());
        selectedRole.setPermission("can_see_dashboard",permCanSeeDashboard.isSelected());
        selectedRole.setPermission("can_see_settings",permCanSeeSettings.isSelected());
        selectedRole.setPermission("can_see_users",permCanSeeUsers.isSelected());
        selectedRole.setPermission("can_see_purchases",permCanSeePurchases.isSelected());
        selectedRole.setPermission("can_edit_purchases",permCanEditPurchases.isSelected());

        roleRepository.saveRole(selectedRole);
    }
    public void initializeTables(){
        loadCategories();
        loadRoles();
    }

    private void handleAddCategory() {
        String newCategory = txtNewCategoryName.getText().trim();

        if (newCategory.isEmpty()) {
            txtNewCategoryName.clear();
            return;
        }

        // Check for existing category case-insensitively
        for (String categoryName : categoryListView.getItems()) {
            if (categoryName.equalsIgnoreCase(newCategory)) {
                txtNewCategoryName.clear();
                return;
            }
        }

        int newId = categoryRepository.addCategory(newCategory);
        if (newId > 0) {
            categoryListView.getItems().add(newCategory);
            txtNewCategoryName.clear();
        }
    }

    private void handleAddRole(){
        // the addRole button will have two functions
        // if the field is empty and there is a selection, we update the permissions of the selection
        // if the field is empty and there is no selection, no action

        String newRole = txtNewRoleName.getText().trim();

        if (newRole.isEmpty()
        ) {
            txtNewCategoryName.clear();
            return;
        }

        // Check for existing category case-insensitively
        for (Role role : roleListView.getItems()) {
            if (role.getRoleName().equalsIgnoreCase(newRole)) {
                txtNewRoleName.clear();
                return;
            }
        }
        Role role = new Role();
        role.setRoleName(txtNewRoleName.getText());
        int newId = roleRepository.addRole(role);
        if(newId > 0){
            role.setRoleId((short)newId);
            roleListView.getItems().add(role);
        }

    }
}
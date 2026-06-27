package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.ICategoryRepository;

import java.net.URL;
import java.util.*;

public class ProductModalController implements Initializable {
    @FXML private Label modalTitleLabel;
    @FXML private TextField txtName;
    @FXML private TextField txtBarcode;
    @FXML private TextField txtCostPrice;
    @FXML private TextField txtRetailPrice;
    @FXML private TextField txtStockQty;
    @FXML private TextField txtMinThreshold;

    @FXML private ComboBox<String> cbCategory;
    @FXML private CheckBox chkActive;

    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private final Map<String, Short> categoryNameToIdMap = new HashMap<>();

    private ICategoryRepository categoryRepository;
    private Product selectedProduct = null;
    private boolean saveClicked = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnSave.setOnAction(e -> handleSaveAction());
        btnCancel.setOnAction(e -> closeWindow());
    }

    private void closeWindow() {
    }

    private void handleSaveAction() {
    }

    public void setCategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void setComboCategoryValues() {
        if (categoryRepository == null) return;
        List<Category> categories = categoryRepository.findAll();
        System.out.println(Arrays.toString(categories.toArray()));
        // Clear the map and combo box to prevent duplicates if recalled
        categoryNameToIdMap.clear();
        this.cbCategory.getItems().clear();

        for (Category cat : categories) {
            categoryNameToIdMap.put(cat.getCategoryName(), cat.getCategoryId());
            this.cbCategory.getItems().add(cat.getCategoryName());
        }

        if (selectedProduct == null || selectedProduct.getCategoryName() == null) {
            this.cbCategory.setValue("Choose a category for the product");
        } else {
            this.cbCategory.setValue(selectedProduct.getCategoryName());
        }
    }

    public void setMode(Product product) {
        this.selectedProduct = product;
        this.saveClicked = false;

        if (product == null) {
            // 🌟 CREATION MODE SETUP
            modalTitleLabel.setText("CREATE NEW PRODUCT ASSET");
            btnSave.setText("Register New Product");
            chkActive.setSelected(true);
            txtName.clear();
            txtBarcode.clear();
            txtCostPrice.setText("0.00");
            txtRetailPrice.setText("0.00");
            txtStockQty.setText("0");
            txtMinThreshold.setText("5");
        } else {
            // 🔒 EDIT MODE SETUP
            modalTitleLabel.setText("PRODUCT SPECIFICATION PROFILE");
            btnSave.setText("Save Registry Changes");

            // Populate form values from entity data
            txtName.setText(product.getProductName());
            txtBarcode.setText(product.getBarcode());
            txtCostPrice.setText(product.getCostPrice().toString());
            txtRetailPrice.setText(product.getRetailPrice().toString());
            //txtStockQty.setText(product.getCurrentQuantity().toString());
            //txtMinThreshold.setText(product.getMinStockLevel().toString());
            chkActive.setSelected(product.isActive());
            cbCategory.setValue(product.getCategoryName());
        }
    }
    public boolean isSaveClicked() {
        return true;
    }

    public Product getProduct() {
        return null ;
    }

}

package org.example.vendly_remastered.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.example.vendly_remastered.cache.ProductsCache;
import org.example.vendly_remastered.cache.model.ProductCacheLine;
import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IBarcodeRepository;
import org.example.vendly_remastered.repository.ICategoryRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.util.*;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class ProductModalController implements Initializable {
    @FXML private Label modalTitleLabel;
    @FXML private TextField txtName;
    @FXML private TextField txtBarcodeEntry;
    @FXML private Button btnAddBarcode;
    @FXML private Button btnRemoveBarcode;
    @FXML private ListView<String> lstBarcodes;
    @FXML private TextField txtCostPrice;
    @FXML private TextField txtRetailPrice;
    @FXML private TextField txtStockQty;
    @FXML private TextField txtMinThreshold;

    @FXML private ComboBox<Category> cbCategory;
    @FXML private CheckBox chkActive;

    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    @FXML private CheckBox chkEnablePromotion;
    @FXML private TextField txtPromoQty;
    @FXML private TextField txtPromoPrice;
    @FXML private HBox promoFieldsContainer;

    private final Map<String, Short> categoryNameToIdMap = new HashMap<>();

    private ICategoryRepository categoryRepository;
    private IBarcodeRepository barcodeRepository;
    private Product selectedProduct = null;
    private List<String> barcodesToDelete = new ArrayList<>();
    public void setBarcodeRepository(IBarcodeRepository repository){ barcodeRepository = repository; }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        txtPromoPrice.managedProperty().bind(chkEnablePromotion.selectedProperty());
        txtPromoPrice.visibleProperty().bind(chkEnablePromotion.selectedProperty());
        txtPromoQty.managedProperty().bind(chkEnablePromotion.selectedProperty());
        txtPromoQty.visibleProperty().bind(chkEnablePromotion.selectedProperty());

        if(!Config.getInstance().isTrackStock()){
            txtStockQty.setText("0.0");
            txtMinThreshold.setText("0.0");
        }
        setDeleteBarcodeBehaviour();
        setBarcodeAddingBehaviour();

        btnSave.setOnAction(e -> handleSaveAction());
        btnCancel.setOnAction(e -> closeWindow());
    }

    private void closeWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private void handleSaveAction() {
        System.out.println("Saving product");
        // 1. Validate inputs FIRST before touching any objects or parsing
        if (!validateInputs()) {
            return;
        }

        try {
            // 2. Safely parse UI inputs
            String name = txtName.getText().trim();
            BigDecimal costPrice = new BigDecimal(txtCostPrice.getText().trim());
            BigDecimal retailPrice = new BigDecimal(txtRetailPrice.getText().trim());
            BigDecimal stockQty = new BigDecimal(txtStockQty.getText().trim());
            BigDecimal minThreshold = new BigDecimal(txtMinThreshold.getText().trim());
            List<String> currentBarcodes = new ArrayList<>(lstBarcodes.getItems());
            BigDecimal promoQty = null;
            BigDecimal promoPrice = null;
            if(chkEnablePromotion.isSelected()){
                 promoQty = chkEnablePromotion.isSelected() ? new BigDecimal(txtPromoQty.getText().trim()) : null;
                 promoPrice = chkEnablePromotion.isSelected() ? new BigDecimal(txtPromoPrice.getText().trim()) : null;
            }

            // 3. Handle object creation vs update depending on selectedProduct
            if (selectedProduct == null) {
                handleCreateNewProduct(name, costPrice, retailPrice, stockQty, minThreshold, currentBarcodes, promoQty, promoPrice);
            } else {
                handleUpdateExistingProduct(name, costPrice, retailPrice, stockQty, minThreshold, currentBarcodes, promoQty, promoPrice);
            }

            closeModal();

        } catch (NumberFormatException e) {
            System.err.println("CRASH CAUSE: " + e.getMessage());
            e.printStackTrace();
            showAlert("Input Error", "Number parsing failed: " + e.getMessage());
            showAlert("Input Error", "Please enter valid numbers for prices and stock quantities.");
        } catch (Exception e) {
            showAlert("Save Error", "An error occurred while saving: " + e.getMessage());
            e.printStackTrace();
        }
    }

// =========================================================================
// VALIDATION & WORKER HELPERS
// =========================================================================

    private boolean validateInputs() {
        if (txtName.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Product Name is required.");
            txtName.requestFocus();
            return false;
        }

        if (cbCategory.getValue() == null) {
            showAlert("Validation Error", "Please select a Category.");
            cbCategory.requestFocus();
            return false;
        }

        // Validate main pricing & stock fields aren't empty
        if (txtCostPrice.getText().trim().isEmpty() || txtRetailPrice.getText().trim().isEmpty()) {
            System.out.println(txtCostPrice.getText() + "  " + txtRetailPrice.getText());
            showAlert("Validation Error", "Cost Price and Retail Price are required.");
            return false;
        }

        // Validate promotion fields if promotion toggle is enabled
        if (chkEnablePromotion.isSelected()) {
            String qty = txtPromoQty.getText().trim();
            String price = txtPromoPrice.getText().trim();
            if (qty.isEmpty() || price.isEmpty()) {
                showAlert("Validation Error", "Please enter both Quantity and Price for the promotion.");
                return false;
            }
        }

        return true;
    }

    private void handleCreateNewProduct(String name, BigDecimal costPrice, BigDecimal retailPrice,
                                        BigDecimal stockQty, BigDecimal minThreshold,
                                        List<String> barcodes, BigDecimal promoQty, BigDecimal promoPrice) {
        Product newProduct = new Product();
        newProduct.setProductName(name);
        newProduct.setCostPrice(costPrice);
        newProduct.setRetailPrice(retailPrice);
        newProduct.setCurrentQuantity(stockQty);
        newProduct.setMinStockLevel(minThreshold);

        newProduct.setCategoryName(cbCategory.getValue().getCategoryName());
        newProduct.setCategoryId(cbCategory.getValue().getCategoryId());
        newProduct.setActive(chkActive.isSelected());
        newProduct.setBarcodes(barcodes);
        if(promoQty != null && promoPrice != null){
            newProduct.setPromotionQuantity(promoQty);
            newProduct.setPromotionPrice(promoPrice);
        }

        // Case A: Fresh product -> All 4 table slices marked dirty for initial DB write
        ProductCacheLine cacheLine = new ProductCacheLine(newProduct, true, true, true, true, System.currentTimeMillis());
        ProductsCache.getInstance().put(cacheLine);
        if(newProduct.getProductId() == -1){
            //TODO: alert product already exists
        }
    }

    private void handleUpdateExistingProduct(String name, BigDecimal costPrice, BigDecimal retailPrice,
                                             BigDecimal stockQty, BigDecimal minThreshold,
                                             List<String> barcodes, BigDecimal promoQty, BigDecimal promoPrice) {
        ProductCacheLine cacheLine = ProductsCache.getInstance().getLine(selectedProduct.getProductId());
        if (cacheLine == null) {
            cacheLine = new ProductCacheLine(selectedProduct, System.currentTimeMillis());
        }

        // Slice 1: Base Product Info Table
        if (!selectedProduct.getProductName().equals(name)
                || selectedProduct.getCostPrice().compareTo(costPrice) != 0
                || selectedProduct.getRetailPrice().compareTo(retailPrice) != 0
                || !selectedProduct.getCategoryName().equals(cbCategory.getValue().getCategoryName())
                || selectedProduct.isActive() != chkActive.isSelected()) {

            selectedProduct.setProductName(name);
            selectedProduct.setCostPrice(costPrice);
            selectedProduct.setRetailPrice(retailPrice);
            selectedProduct.setCategoryName(cbCategory.getValue().getCategoryName());
            selectedProduct.setCategoryId(cbCategory.getValue().getCategoryId());
            selectedProduct.setActive(chkActive.isSelected());

            cacheLine.setDirtyBit(true, ProductCacheLine.PRODUCT_BIT);
        }

        // Slice 2: Stock Table
        if (selectedProduct.getCurrentQuantity().compareTo(stockQty) != 0
                || selectedProduct.getMinStockLevel().compareTo(minThreshold) != 0) {

            selectedProduct.setCurrentQuantity(stockQty);
            selectedProduct.setMinStockLevel(minThreshold);

            cacheLine.setDirtyBit(true, ProductCacheLine.STOCK_BIT);
        }

        // Slice 3: Barcodes Table
        if (!selectedProduct.getBarcodes().equals(barcodes)) {
            selectedProduct.setBarcodes(barcodes);

            cacheLine.setDirtyBit(true, ProductCacheLine.BARCODE_BIT);
            if(!barcodesToDelete.isEmpty()){
                barcodeRepository.deleteBarcode(barcodesToDelete);
                barcodesToDelete.clear();
            }
        }

        // Slice 4: Promotion Table
        if (hasPromotionChanged(promoQty, promoPrice)) {
            selectedProduct.setPromotionQuantity(promoQty);
            selectedProduct.setPromotionPrice(promoPrice);
            cacheLine.setDirtyBit(true, ProductCacheLine.PROMO_BIT);
        }

        // Push update if at least one bit changed
        if (cacheLine.isDirtyBit()) {
            cacheLine.setProduct(selectedProduct);
            cacheLine.setCacheVersion(cacheLine.getCacheVersion() + 1);
            ProductsCache.getInstance().put(cacheLine);
        }
    }

    private boolean hasPromotionChanged(BigDecimal newQty, BigDecimal newPrice) {
        if (newQty == null || newPrice == null) {
            return selectedProduct.hasPromotion();
        }
        if (!selectedProduct.hasPromotion()) {
            return true;
        }
        return selectedProduct.getPromotionQuantity().compareTo(newQty) != 0
                || selectedProduct.getPromotionPrice().compareTo(newPrice) != 0;
    }
    private void closeModal() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    public void setCategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void setComboCategoryValues() {
        if (categoryRepository == null) return;

        List<Category> categories = categoryRepository.findAll();

        categoryNameToIdMap.clear();
        this.cbCategory.getItems().clear();

        for (Category cat : categories) {
            categoryNameToIdMap.put(cat.getCategoryName(), cat.getCategoryId());
            this.cbCategory.getItems().add(cat); // Add Category object, not String
        }

        // Set prompt text instead of inserting a fake String into a ComboBox<Category>
        this.cbCategory.setPromptText("Choose a category for the product");

        if (selectedProduct != null && selectedProduct.getCategoryName() != null) {
            // Find matching Category object from loaded list
            categories.stream()
                    .filter(cat -> cat.getCategoryName().equalsIgnoreCase(selectedProduct.getCategoryName()))
                    .findFirst()
                    .ifPresent(matchingCat -> this.cbCategory.setValue(matchingCat));
        } else {
            this.cbCategory.setValue(null);
        }
    }

    public void setMode(Product product, boolean READ_ONLY) {
        // Create a boolean property or flag to represent your modal's read-only state
        BooleanProperty isReadOnlyProp = new SimpleBooleanProperty(READ_ONLY);

        // Combine stock tracking with the read-only state for stock fields:
        txtStockQty.editableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> Config.getInstance().trackStockProperty().get() && !isReadOnlyProp.get(),
                        Config.getInstance().trackStockProperty(), isReadOnlyProp
                )
        );

        txtMinThreshold.editableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> Config.getInstance().trackStockProperty().get() && !isReadOnlyProp.get(),
                        Config.getInstance().trackStockProperty(), isReadOnlyProp
                )
        );

        this.selectedProduct = product;

        if (product == null) {
            // 🌟 CREATION MODE SETUP
            modalTitleLabel.setText("CREATE NEW PRODUCT ASSET");
            btnSave.setText("Register New Product");
            chkActive.setSelected(true);
            txtName.clear();
            txtBarcodeEntry.clear();
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
            lstBarcodes.getItems().addAll(product.getBarcodes());
            txtCostPrice.setText(product.getCostPrice().toString());
            txtRetailPrice.setText(product.getRetailPrice().toString());
            txtStockQty.setText(product.getCurrentQuantity().toString());
            txtMinThreshold.setText(product.getMinStockLevel().toString());
            chkActive.setSelected(product.isActive());

            if (product.getCategoryName() != null) {
                cbCategory.getItems().stream()
                        .filter(cat -> cat.getCategoryName().equalsIgnoreCase(product.getCategoryName()))
                        .findFirst()
                        .ifPresentOrElse(
                                cbCategory::setValue,
                                () -> cbCategory.setValue(null)
                        );
            } else {
                cbCategory.setValue(null);
            }

            if (product.hasPromotion()) {
                promoFieldsContainer.setVisible(true);
                promoFieldsContainer.setManaged(true);
                chkEnablePromotion.setSelected(true);
                txtPromoQty.setText(product.getPromotionQuantity().toString());
                txtPromoPrice.setText(product.getPromotionPrice().toString());
            } else {
                promoFieldsContainer.setVisible(false);
                promoFieldsContainer.setManaged(false);
            }
        }

        // 🔒 Apply READ_ONLY restrictions LAST so they override everything else cleanly
        if (READ_ONLY) {
            btnSave.setVisible(false);
            btnCancel.setVisible(false);
            txtBarcodeEntry.setVisible(false);
            btnAddBarcode.setVisible(false);
            txtName.setEditable(false);
            txtRetailPrice.setEditable(false);
            txtPromoPrice.setEditable(false);
            txtPromoQty.setEditable(false);
            txtCostPrice.setEditable(false);
            chkActive.setDisable(true);
            chkEnablePromotion.setDisable(true);
            btnRemoveBarcode.setVisible(false);
            cbCategory.setDisable(true); // Note: ComboBoxes use setDisable(true) better than setEditable(false)
        }
    }
    public boolean isSaveClicked() {
        return true;
    }

    public Product getProduct() {
        return null ;
    }

    private void setBarcodeAddingBehaviour(){
        txtBarcodeEntry.setTextFormatter(TextFormatterUtils.createBarcodeFormatter());
        btnAddBarcode.setOnAction(_ -> {
            String barcode = txtBarcodeEntry.getText().trim();
            if(barcode.isEmpty())
                return;
            // Prevent duplicate entries in the UI list
            if (lstBarcodes.getItems().contains(barcode))
                return;

            lstBarcodes.getItems().add(barcode);
            barcodesToDelete.remove(barcode);

            txtBarcodeEntry.clear(); // Ready for next scan/entry
        });

    }

    private void setDeleteBarcodeBehaviour(){
        btnRemoveBarcode.setOnAction(_ -> {
            String targetBarcode = lstBarcodes.getSelectionModel().getSelectedItem();
            lstBarcodes.getItems().remove(targetBarcode);
            barcodesToDelete.add(targetBarcode);

        });
    }
}

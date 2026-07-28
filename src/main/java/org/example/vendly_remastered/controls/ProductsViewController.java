package org.example.vendly_remastered.controls;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.models.entity.Stock;
import org.example.vendly_remastered.repository.ICategoryRepository;
import org.example.vendly_remastered.repository.IProductRepository;
import org.example.vendly_remastered.repository.IStockRepository;
import org.example.vendly_remastered.utils.Config;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ProductsViewController implements Initializable {
    @FXML private Label selectedProdInfoLabel;
    @FXML private TextArea productDetailsArea;
    @FXML private Label searchRegistryLabel;
    @FXML private TextField searchField;
    @FXML private Label categoryLabel;
    @FXML private ComboBox<Category> categoryFilterComboBox;
    @FXML private Button addProductButton;
    @FXML private Button productDetailsButton;
    @FXML private Button adjustStockButton;
    @FXML private TableView<Product> productTableView; // Replace ? with your real Product model object

    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, BigDecimal> colCostPrice;
    @FXML private TableColumn<Product, BigDecimal> colRetailPrice;
    @FXML private TableColumn<Product, BigDecimal> colStockQty;
    @FXML private TableColumn<Product, BigDecimal> colMinThreshold;
    @FXML private TableColumn<Product, Boolean> colStatus;

    private IProductRepository productRepository;
    private ICategoryRepository categoryRepository;
    private IStockRepository stockRepository;
    private final ObservableList<Product> masterData = FXCollections.observableArrayList();
    private FilteredList<Product> filteredData;
    private SortedList<Product> sortedData;

    public void setCategoryRepository(ICategoryRepository categoryRepository){ this.categoryRepository = categoryRepository; }
    public void setProductRepository(IProductRepository productRepository){ this.productRepository = productRepository; }
    public void setStockRepository(IStockRepository stockRepository){ this.stockRepository = stockRepository; }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
        setButtonsBehaviour();
        setSearchBarBehaviour();
        setTableBehaviour();
    }

    private void setTableColumnsBehaviour(){
        colSku.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCostPrice.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        colRetailPrice.setCellValueFactory(new PropertyValueFactory<>("retailPrice"));
        colStockQty.setCellValueFactory(new PropertyValueFactory<>("currentQuantity"));
        colMinThreshold.setCellValueFactory(new PropertyValueFactory<>("minStockLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("active"));

        // 🎨 Aesthetic polish for status column rendering
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Product p = getTableRow().getItem();
                    setText(p.isActive() ? "ACTIVE" : "DISABLED");
                    setStyle(p.isActive() ? "-fx-text-fill: #2e7d32; -fx-font-weight: bold;" : "-fx-text-fill: #757575;");
                }
            }
        });
    }

    private void setTableBehaviour(){}
    public void initializeTable() {
        loadProductData();

        // 1. Setup wrappers using the class-level masterData list
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);

        // 2. Bind the SortedList comparator to the TableView's comparator properties
        sortedData.comparatorProperty().bind(productTableView.comparatorProperty());
        productTableView.setItems(sortedData);

        // 3. Connect live search field mutations to our multi-criteria routing filter
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyCombinedFilter();
        });
    }

    public void setSearchBarBehaviour() {
        Platform.runLater(() -> searchField.requestFocus());

        searchField.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE -> {
                    searchField.clear();
                    event.consume();
                }
                case ENTER -> {
                    // Fixed: Updated target collection variable from tvProducts to productTableView
                    if (productTableView.getItems().size() == 1) {
                        productTableView.getSelectionModel().selectFirst();
                        productTableView.requestFocus();
                    }
                    event.consume();
                }
            }
        });
    }

    // 🎯 Call this target routine from your Application initial load loop or structural dependencies hook
    public void loadProductData() {
        if (productRepository == null) return;
        List<Product> dataList = productRepository.findAll();
        Map<Integer, Stock> stockMap = stockRepository.findAll();
        for(Product prod: dataList){
            Stock stock = stockMap.get(prod.getProductId());
            if(stock != null){
                prod.setCurrentQuantity(stock.getCurrentStock());
                prod.setMinStockLevel(stock.getMinThreshold());
            }
        }
        masterData.setAll(dataList);
    }

// Make sure this is declared at the class-level alongside your product repository:
// @Inject or manually set: private ICategoryRepository categoryRepository;

    public void loadCategories() {
        if (categoryRepository == null) return;

        // 1. Fetch categories from your DB lookup routine
        List<Category> cl = categoryRepository.findAll();
        System.out.println(Arrays.toString(cl.toArray()));
        ObservableList<Category> comboItems = FXCollections.observableArrayList();

        // 2. Add an explicit 'All Categories' anchor entry at the top of the list
        Category allChoice = new Category();
        allChoice.setCategoryId((short) -1);
        allChoice.setCategoryName("All Categories");

        comboItems.add(allChoice);
        comboItems.addAll(cl);

        // 3. Bind elements to the UI component and select the first item by default
        categoryFilterComboBox.setItems(comboItems);
        categoryFilterComboBox.getSelectionModel().selectFirst();

        // 4. Listen for selection mutations to trigger the combined filter evaluation
        categoryFilterComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            applyCombinedFilter();
        });
    }

    /**
     * Evaluates both the search field text and the category combo box choice simultaneously.
     */
    private void applyCombinedFilter() {
        if (filteredData == null) return;

        filteredData.setPredicate(product -> {
            // --- 1. Evaluate Text Search Filter ---
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isBlank()) {
                String filter = searchText.toLowerCase().trim();
                boolean matchesSearch = (product.getProductName() != null && product.getProductName().toLowerCase().contains(filter)) ||
                        (product.getCategoryName() != null && product.getCategoryName().toLowerCase().contains(filter)) ||
                        (product.getBarcode() != null && product.getBarcode().contains(filter)) ||
                        (product.getBarcodes() != null && product.getBarcodes().stream().anyMatch(b -> b.contains(filter)));

                if (!matchesSearch) return false; // Fails text filter constraint
            }

            // --- 2. Evaluate Category Dropdown Filter ---
            Category selectedCategory = categoryFilterComboBox.getSelectionModel().getSelectedItem();
            if (selectedCategory == null || selectedCategory.getCategoryId() == -1) {
                return true; // "All Categories" passes tracking checks natively
            }

            return product.getCategoryId() == selectedCategory.getCategoryId();
        });
    }

    private void openProductModal(Product product) {
        try {
            Config config = Config.getInstance();
            // 1. Point to your wide layout FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/ProductDetailsModal.fxml"));
            loader.setResources(config.getLanguageBundle());
            Parent root = loader.load();

            config.applyOrientation(root);
            // 2. Dispatch dependencies (injects category repos, services, etc., via your framework dispatcher)
            Object controller = loader.getController();
            ControllerDispatcher.execute(controller);

            ProductModalController modalController = loader.getController();

            // 4. Check if we are editing an existing product or adding a completely fresh item
            boolean isEditMode = (product != null);
            if (isEditMode) {
                modalController.setMode(product);
            }

            // 5. Build and present the window scene configuration
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(isEditMode ? "Edit Product Specifications" : "Register New Product Asset");
            modalStage.setScene(new Scene(root));

            // Execution pauses right here until the user clicks Save or Cancel
            modalStage.showAndWait();

            // 🌟 THE HANDOFF: Execution safely resumes here once the window closes
            if (modalController.isSaveClicked()) {
                // 6. Reload table arrays completely to cleanly display your mutated records
                loadProductData();
            }

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
            alert.showAndWait();
            ex.printStackTrace();
        }
    }

    private void setButtonsBehaviour() {
        // 🔍 1. View Product Details Action
        this.productDetailsButton.setOnAction(_ -> {
            Product selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Open modal in edit/view mode using your established pattern
                openProductModal(selected);
            } else {
                showSelectionWarning("Product Details");
            }
        });

        // ➕ 2. Add Product Button (Assuming it's addProductBottun from your fields)
        this.addProductButton.setOnAction(_ -> {
            // Passing null opens the modal in fresh creation mode
            openProductModal(null);
        });

        // 📉 3. Adjust Stock Action
        this.adjustStockButton.setOnAction(_ -> {
            Product selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // Placeholder: Call your upcoming stock/inventory control window here
                System.out.println("Adjusting stock levels for: " + selected.getProductName());
            } else {
                showSelectionWarning("Adjust Stock");
            }
        });
    }

    // 🛑 Quick UI Helper to keep code clean and prevent alert code repetition
    private void showSelectionWarning(String actionName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Selection Found");
        alert.setHeaderText(actionName + " Action Require Choice");
        alert.setContentText("Please highlight an active product row from the table list first.");
        alert.showAndWait();
    }
}

package org.example.vendly_remastered.controls;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.Category;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ICategoryRepository;
import org.example.vendly_remastered.repository.IProductRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
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
    @FXML private TableColumn<Product, Integer> colStockQty;
    @FXML private TableColumn<Product, Integer> colMinThreshold;
    @FXML private TableColumn<Product, Boolean> colStatus;

    private IProductRepository productRepository;
    private ICategoryRepository categoryRepository;

    private final ObservableList<Product> masterData = FXCollections.observableArrayList();
    private FilteredList<Product> filteredData;
    private SortedList<Product> sortedData;
    public void setCategoryRepository(ICategoryRepository categoryRepository){ this.categoryRepository = categoryRepository; }
    public void setProductRepository(IProductRepository productRepository){ this.productRepository = productRepository; }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
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
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyCombinedFilter();
        });
        // 3. Connect live search field listener mutations to the FilteredList predicate
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(product -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String filter = newValue.toLowerCase().trim();

                if (product.getProductName() != null && product.getProductName().toLowerCase().contains(filter)) return true;
                if (product.getCategoryName() != null && product.getCategoryName().toLowerCase().contains(filter)) return true;
                if (product.getBarcode() != null && product.getBarcode().contains(filter)) return true;
                return product.getBarcodes() != null && product.getBarcodes().stream().anyMatch(b -> b.contains(filter));
            });
        });
    }

    public void setupSearchBarBehaviour() {
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
}

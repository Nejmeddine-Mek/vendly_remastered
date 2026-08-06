package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class NoBarCodeSelectionController implements Initializable {
    @FXML private VBox contentContainer;
    @FXML private TextField searchBar;
    @FXML private VBox categoryRowsContainer;

    List<Product> allUncodedProducts;
    // Master map containing all categories and all their original products
    private final Map<String, List<Product>> masterCategoryProducts = new HashMap<>();

    // The active map you are currently rendering from
    private final Map<String, ListView<Product>> categoryListViews = new HashMap<>();
    private IProductRepository productRepository;

    public void setProductRepository(IProductRepository productRepository){ this.productRepository = productRepository; }
    private Consumer<Product> onProductSelectedCallback;

    // Setter so whoever opens this view can pass a handler to it
    private Consumer<Product> onProductSelected;

    public void setOnClientSelected(Consumer<Product> onSupplierSaved) {
        this.onProductSelected = onSupplierSaved;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void initializeLists(){
        loadUncodedProducts();
    }

    private void loadUncodedProducts() {
        this.allUncodedProducts = productRepository.getUncodedProducts();
        if (allUncodedProducts.isEmpty())
            return;

        masterCategoryProducts.clear();
        for (Product product : allUncodedProducts) {
            masterCategoryProducts
                    .computeIfAbsent(product.getCategoryName(), k -> new ArrayList<>())
                    .add(product);
        }

        categoryListViews.clear();

        // Manually create each ListView and configure it once
        for (var entry : masterCategoryProducts.entrySet()) {
            String categoryName = entry.getKey();
            List<Product> products = entry.getValue();

            ListView<Product> categoryList = new ListView<>();
            categoryList.getItems().addAll(products);
            categoryList.getStyleClass().add("custom-list-view");

            // 1. Keep only the cell factory for formatting text
            categoryList.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Product product, boolean empty) {
                    super.updateItem(product, empty);
                    if (empty || product == null) {
                        setText(null);
                    } else {
                        setText(product.getProductName() + " — " + product.getRetailPrice() + " DA");
                    }
                }
            });

            // 2. Handle double-clicks directly on the ListView using selection model
            categoryList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    Product selectedProduct = categoryList.getSelectionModel().getSelectedItem();
                    if (selectedProduct != null && onProductSelectedCallback != null) {
                        onProductSelectedCallback.accept(selectedProduct);
                        closeModal();
                    }
                }
            });

            categoryListViews.put(categoryName, categoryList);
        }

        buildCategoryUI();
        setSearchbarBehaviour();
    }
    private void buildCategoryUI() {
        categoryRowsContainer.getChildren().clear(); // Clear only the category rows, leaving search bar safe!

        if (categoryListViews.isEmpty()) {
            return;
        }

        var entries = new java.util.ArrayList<>(categoryListViews.entrySet());

        for (int i = 0; i < entries.size(); i += 2) {
            HBox rowBox = new HBox(15);
            rowBox.getStyleClass().add("category-row-box");
            rowBox.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(rowBox, Priority.ALWAYS);

            // First category in the pair
            var entry1 = entries.get(i);
            VBox categorySection1 = createCategorySection(entry1.getKey(), entry1.getValue());
            HBox.setHgrow(categorySection1, Priority.ALWAYS);
            rowBox.getChildren().add(categorySection1);

            // Second category in the pair (if it exists)
            if (i + 1 < entries.size()) {
                var entry2 = entries.get(i + 1);
                VBox categorySection2 = createCategorySection(entry2.getKey(), entry2.getValue());
                HBox.setHgrow(categorySection2, Priority.ALWAYS);
                rowBox.getChildren().add(categorySection2);
            } else {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                rowBox.getChildren().add(spacer);
            }

            categoryRowsContainer.getChildren().add(rowBox);
        }
    }

    private VBox createCategorySection(String categoryName, ListView<Product> listView) {
        VBox categorySection = new VBox(5);
        categorySection.getStyleClass().add("category-section-box");
        categorySection.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(categoryName.toUpperCase());
        titleLabel.getStyleClass().add("category-section-title");

        int itemCount = listView.getItems().size();
        listView.setPrefHeight(Math.min(itemCount * 35 + 10, 150));
        listView.setMaxWidth(Double.MAX_VALUE);

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (empty || product == null) {
                    setText(null);
                } else {
                    setText(product.getProductName() + " — " + product.getRetailPrice() + " DA");
                }
            }
        });

        categorySection.getChildren().addAll(titleLabel, listView);
        return categorySection;
    }

    private void setSearchbarBehaviour() {
        if (searchBar == null) return;

        searchBar.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();

            // Clear current active views map
            categoryListViews.clear();

            // Filter master data
            for (var entry : masterCategoryProducts.entrySet()) {
                String categoryName = entry.getKey();
                List<Product> allProductsInCat = entry.getValue();

                // Filter products matching either the product name or category name
                List<Product> filteredProducts = allProductsInCat.stream()
                        .filter(p -> p.getProductName().toLowerCase().contains(query) ||
                                categoryName.toLowerCase().contains(query))
                        .toList();

                // If there are matches, recreate/update the ListView for this category
                if (!filteredProducts.isEmpty()) {
                    ListView<Product> categoryList = new ListView<>();
                    categoryList.getItems().addAll(filteredProducts);
                    categoryListViews.put(categoryName, categoryList);
                }
            }

            // Rebuild the UI grid with the filtered results
            buildCategoryUI();
        });
    }
    private void closeModal() {
        if (categoryRowsContainer != null && categoryRowsContainer.getScene() != null) {
            Stage stage = (Stage) categoryRowsContainer.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}

package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class NoBarCodeSelectionController implements Initializable {
    @FXML private VBox contentContainer;
    @FXML private TextField searchBar;


    List<Product> allUncodedProducts;
    private final Map<String, ListView<Product>> categoryListViews = new HashMap<>();

    private IProductRepository productRepository;

    public void setProductRepository(IProductRepository productRepository){ this.productRepository = productRepository; }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void initializeLists(){
        loadUncodedProducts();
    }

    private void loadUncodedProducts(){
        this.allUncodedProducts = productRepository.getUncodedProducts();
        if(allUncodedProducts.isEmpty())
            return;
        for(Product product : allUncodedProducts){
            if(categoryListViews.get(product.getCategoryName()) == null){
                ListView<Product> categoryList = new ListView<>();
                categoryList.getItems().add(product);
                categoryListViews.put(product.getCategoryName(), categoryList);
            } else {
                categoryListViews.get(product.getCategoryName()).getItems().add(product);
            }
        }
        buildCategoryUI();

    }

    private void buildCategoryUI() {
        contentContainer.getChildren().clear(); // Clear old items if reloading

        categoryListViews.forEach((categoryName, listView) -> {
            VBox categorySection = new VBox(5);
            categorySection.getStyleClass().add("category-section-box");

            // Category Header Title
            Label titleLabel = new Label(categoryName.toUpperCase());
            titleLabel.getStyleClass().add("category-section-title");

            // Style/Configure the ListView
            int itemCount = listView.getItems().size();
            listView.setPrefHeight(Math.min(itemCount * 35 + 10, 150)); // Dynamic height limit
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

            // Add elements to the section container, then to the main scrollable view
            categorySection.getChildren().addAll(titleLabel, listView);
            contentContainer.getChildren().add(categorySection);
        });
    }
}

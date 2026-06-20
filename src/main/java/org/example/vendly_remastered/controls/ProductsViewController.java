package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ProductsViewController {
    @FXML private Label selectedProdInfoLabel;
    @FXML private TextArea productDetailsArea;
    @FXML private Label searchRegistryLabel;
    @FXML private TextField searchField;
    @FXML private Label categoryLabel;
    @FXML private ComboBox<String> categoryFilterComboBox;
    @FXML private Button addProductBottun;
    @FXML private Button productDetailsButton;
    @FXML private Button adjustStockButton;
    @FXML private TableView<?> productTableView; // Replace ? with your real Product model object

    @FXML private TableColumn<?, String> colSku;
    @FXML private TableColumn<?, String> colName;
    @FXML private TableColumn<?, String> colCategory;
    @FXML private TableColumn<?, Double> colCostPrice;
    @FXML private TableColumn<?, Double> colRetailPrice;
    @FXML private TableColumn<?, Integer> colStockQty;
    @FXML private TableColumn<?, Integer> colMinThreshold;
    @FXML private TableColumn<?, String> colStatus;
}

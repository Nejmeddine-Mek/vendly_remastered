package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SuppliersViewController {
    @FXML private Label supplierInfo;
    @FXML private TextArea supplierDetailsArea;
    @FXML private Label searchSuppliersLabel;
    @FXML private TextField searchField;
    @FXML private Button addSupplierButton;
    @FXML private Button supplierDetailsButton;
    @FXML private Button processOrderButton;
    @FXML private TableView<?> supplierTableView; // Replace ? with your Supplier model data entity

    @FXML private TableColumn<?, Integer> colId;
    @FXML private TableColumn<?, String> colCompanyName;
    @FXML private TableColumn<?, String> colContactName;
    @FXML private TableColumn<?, String> colPhone;
    @FXML private TableColumn<?, String> colEmail;
    @FXML private TableColumn<?, Integer> colTotalOrders;
    @FXML private TableColumn<?, Double> colBalanceOwed;
}

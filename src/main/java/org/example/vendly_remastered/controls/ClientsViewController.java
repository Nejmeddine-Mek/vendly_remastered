package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ClientsViewController {
    @FXML private Label clientDetailsLabel;
    @FXML private TextArea clientDetailsArea;
    @FXML private Label clientSearchLabel;
    @FXML private TextField searchField;
    @FXML private Button addClientButton;
    @FXML private Button clientDetailsButton;
    @FXML private Button processPaymentButton;

    @FXML private TableView<?> clientTableView; // Replace ? with your Client Object Model

    @FXML private TableColumn<?, Integer> colId;
    @FXML private TableColumn<?, String> colFirstName;
    @FXML private TableColumn<?, String> colLastName;
    @FXML private TableColumn<?, String> colPhone;
    @FXML private TableColumn<?, String> colEmail;
    @FXML private TableColumn<?, Integer> colPurchases;
    @FXML private TableColumn<?, Double> colDebts;
}

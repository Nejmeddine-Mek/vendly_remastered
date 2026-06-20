package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.lang.classfile.Label;

public class HistoryViewController {
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<String> salesTable;
    @FXML private TableColumn<String, String> colSaleId;
    @FXML private TableColumn<String, String> colDate;
    @FXML private TableColumn<String, String> colClient;
    @FXML private TableColumn<String, String> colTotal;
    @FXML private Label transactionSummary;
    @FXML private Label employeeLabel;
    @FXML private TextField txtCashier;
    @FXML private Label profitLabel;
    @FXML private TextField txtProfit;
    @FXML private Button btnOpenDetails;
}

package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;


public class HistoryViewController implements Initializable {
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<SaleHeader> salesTable;
    @FXML private TableColumn<SaleHeader, Long> colSaleId;
    @FXML private TableColumn<SaleHeader, LocalDateTime> colDate;
    @FXML private TableColumn<SaleHeader, LocalDateTime> colClient;
    @FXML private TableColumn<SaleHeader, BigDecimal> colTotal;
    @FXML private Label transactionSummary;
    @FXML private Label employeeLabel;
    @FXML private TextField txtCashier;
    @FXML private Label profitLabel;
    @FXML private TextField txtProfit;
    @FXML private Button btnOpenDetails;

    private ISalesHeaderRepository salesHeaderRepository;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
    public void setTableColumnsBehaviour(){
        this.colSaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        this.colClient.setCellValueFactory(new PropertyValueFactory<>("saleClientName"));
        this.colDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        this.colTotal.setCellValueFactory(new PropertyValueFactory<>("saleTotal"));
    }

    public void setTableViewBehaviour(){

    }
    public void setSearchBarBehaviour(){

    }
    public void initializeTable(){

    }
}

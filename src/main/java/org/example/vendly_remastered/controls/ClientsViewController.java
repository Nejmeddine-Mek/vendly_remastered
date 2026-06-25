package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.repository.IClientsRepository;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientsViewController implements Initializable {
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

    private IClientsRepository clientsRepository;

    public void setClientsRepository(IClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
    }

    private void setTableColumnsBehaviour(){
        colId.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("contactFullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPurchases.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        colDebts.setCellValueFactory(new PropertyValueFactory<>("currentDebts"));
    }

    public void initializeTable(){

    }
}

package org.example.vendly_remastered.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.ISuppliersRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class SuppliersViewController implements Initializable {
    @FXML private Label supplierInfo;
    @FXML private TextArea supplierDetailsArea;
    @FXML private Label searchSuppliersLabel;
    @FXML private TextField searchField;
    @FXML private Button addSupplierButton;
    @FXML private Button supplierDetailsButton;
    @FXML private Button processOrderButton;
    @FXML private TableView<Supplier> supplierTableView; // Replace ? with your Supplier model data entity

    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String> colCompanyName;
    @FXML private TableColumn<Supplier, String> colContactName;
    @FXML private TableColumn<Supplier, String> colPhone;
    @FXML private TableColumn<Supplier, String> colEmail;
    @FXML private TableColumn<Supplier, String> colCreatedBy;
    @FXML private TableColumn<Supplier, BigDecimal> colBalanceOwed;

    ISuppliersRepository suppliersRepository;

    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    public void setSuppliersRepository(ISuppliersRepository suppliersRepository){ this.suppliersRepository = suppliersRepository; }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
    }
    private void setTableColumnsBehaviour() {
        colId.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        colCompanyName.setCellValueFactory(new PropertyValueFactory<>("supplierCompany"));
        colContactName.setCellValueFactory(new PropertyValueFactory<>("supplierFullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Displays createdBy integer directly (or username if Supplier model holds it)
        colCreatedBy.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getCreatedBy() != null ? cell.getValue().getCreatedBy().toString() : "—"
                )
        );

        // Formats BigDecimal debts cleanly as currency (e.g. 150000.00 DA)
        colBalanceOwed.setCellValueFactory(new PropertyValueFactory<>("debts"));
        colBalanceOwed.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DA", balance));
                }
            }
        });
    }

    public void initializeTable() {
        supplierTableView.setItems(supplierList);

        // Fetch initial dataset if repository is already injected
        if (suppliersRepository != null) {
            supplierList.setAll(suppliersRepository.findAll());
        }
    }
}

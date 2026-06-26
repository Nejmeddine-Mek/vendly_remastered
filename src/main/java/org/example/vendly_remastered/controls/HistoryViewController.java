package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;


public class HistoryViewController implements Initializable {
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<SaleHeader> salesTable;
    @FXML private TableColumn<SaleHeader, Integer> colSaleId;
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
    private ObservableList<SaleHeader> salesList;
    private FilteredList<SaleHeader> filteredSales;

    public void setSalesHeaderRepository(ISalesHeaderRepository salesHeaderRepository){
        this.salesHeaderRepository = salesHeaderRepository;
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableViewBehaviour();
        setSearchBarBehaviour();
    }

    public void setTableViewBehaviour() {
        // 🎯 Listen for row changes in your JavaFX TableView
        salesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Populate fields instantly with the selected row data
                updateUIWithSelectedSale(newSelection);
            } else {
                // Clear or reset fields safely if selection is lost
                clearUIFields();
            }
        });
    }

    /**
     * Maps the properties of the selected SaleHeader straight to your FXML nodes.
     */
    private void updateUIWithSelectedSale(SaleHeader sale) {
        // 1. Transaction Summary Label (Perfect for highlighting ID and transaction type)
        transactionSummary.setText(String.format("TX #%d — %s", sale.getSaleId(), sale.getSaleNote().toUpperCase()));

        // 2. Employee Info Mapping
        employeeLabel.setText("Served By:");
        txtCashier.setText(sale.getSaleUsername()); // Uses transient name pulled from JOIN

        // 3. Profit Analytics Mapping (Formatted to 2 decimal places manually for accuracy)
        profitLabel.setText("Net Profit:");
        txtProfit.setText(String.format("%.2f DA", sale.getSaleProfit()));

        // Optional extension: You can display total summary contextual data here too
        // e.g., "Total: %.2f | Paid: %.2f"
    }

    /**
     * Resets the UI components safely if a user clicks empty space or clears filters.
     */
    private void clearUIFields() {
        transactionSummary.setText("No Transaction Selected");
        employeeLabel.setText("Employee:");
        txtCashier.clear();
        profitLabel.setText("Profit:");
        txtProfit.clear();
    }

    public void loadInitialSalesTableData() {
        if (salesHeaderRepository == null) return;

        // 1. Instantiate the observable list wrapper exactly like your pattern
        this.salesList = FXCollections.observableArrayList();

        // 2. Fetch today's records by default to keep performance tight
        List<SaleHeader> sl = salesHeaderRepository.findByDate(LocalDate.now());
        this.salesList.addAll(sl);

        // 3. Set up the FilteredList wrapper
        this.filteredSales = new FilteredList<>(this.salesList, p -> true);

        // 4. Bind to the UI TableView and refresh
        salesTable.setItems(this.filteredSales);
        salesTable.refresh();
    }

    public void setSearchBarBehaviour() {
        if (this.filteredSales == null) return;

        // 🎯 Listen to real-time keystrokes inside the search field
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            this.filteredSales.setPredicate(sale -> {
                // Rule 1: If search bar is blank, show all rows for the active date
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String searchKeyword = newValue.toLowerCase().trim();

                // Rule 2: Check matching criteria across your data model properties

                // Match 1: Transaction ID
                if (String.valueOf(sale.getSaleId()).contains(searchKeyword)) {
                    return true;
                }

                // Match 2: Cashier Username (Transient property from User JOIN)
                if (sale.getSaleUsername() != null && sale.getSaleUsername().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                // Match 3: Client Display Name (Transient property from Client LEFT JOIN)
                if (sale.getClientName() != null && sale.getClientName().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                // Match 4: Transaction Type note ('sale', 'return', 'expired')
                if (sale.getSaleNote() != null && sale.getSaleNote().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                return false; // Hide row if it doesn't match any criteria
            });
        });

        // Optional: Refresh the TableView UI layout to reflect structural changes visually
        salesTable.refresh();
    }
}

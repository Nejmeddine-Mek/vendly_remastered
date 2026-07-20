package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISaleDetailsRepository;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;


public class HistoryViewController implements Initializable {
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<SaleHeader> salesTable;
    @FXML private TableColumn<SaleHeader, Integer> colSaleId;
    @FXML private TableColumn<SaleHeader, LocalDateTime> colDate;
    @FXML private TableColumn<SaleHeader, String> colClient;
    @FXML private TableColumn<SaleHeader, BigDecimal> colTotal;
    @FXML private TableColumn<SaleHeader, BigDecimal> colPaid;
    @FXML private TextField txtSelectedSaleId;
    @FXML private TextField txtSelectedDate;
    @FXML private TextField txtSelectedClient;
    @FXML private TextField txtSelectedCashier;
    @FXML private TextField txtSelectedDebt;
    @FXML private TextField txtSelectedProfit;
    @FXML private TextField txtTotalSalesCount;
    @FXML private TextField txtTotalCashFlow;
    @FXML private TextField txtTheoreticalProfit;

    @FXML private Label transactionSummary;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button btnFilterDate;
    @FXML private Button btnClearFilter;
    @FXML private Button btnOpenDetails;
    private ISaleDetailsRepository saleDetailsRepository;
    private ISalesHeaderRepository salesHeaderRepository;
    private ObservableList<SaleHeader> salesList;
    private FilteredList<SaleHeader> filteredSales;
    public void setSaleDetailsRepository(ISaleDetailsRepository sdr){ saleDetailsRepository = sdr; }
    public void setSalesHeaderRepository(ISalesHeaderRepository salesHeaderRepository){
        this.salesHeaderRepository = salesHeaderRepository;
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableViewBehaviour();
        setSearchBarBehaviour();
        setTableColumnsBehaviour();
        setSaleDetailsButton();
        btnFilterDate.setOnAction(_ -> setDomainSearchButton() );
    }
    public void setTableColumnsBehaviour(){
        colSaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("saleTotal"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
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
        txtSelectedSaleId.setText(String.valueOf(sale.getSaleId()));
        txtSelectedDate.setText(String.valueOf(sale.getSaleDate()));
        txtSelectedClient.setText(sale.getClientName() == null ? "N/A":sale.getClientName());
        txtSelectedCashier.setText(sale.getSaleUsername());
        txtSelectedProfit.setText(String.valueOf(sale.getSaleProfit()));
        txtSelectedDebt.setText(String.valueOf(sale.getRemainingDebt()));
        transactionSummary.setText(String.format("TX #%d — %s", sale.getSaleId(), sale.getSaleNote().toUpperCase()));
/*
        // 2. Employee Info Mapping
        employeeLabel.setText("Served By:");
        txtCashier.setText(sale.getSaleUsername()); // Uses transient name pulled from JOIN
*/
        // 3. Profit Analytics Mapping (Formatted to 2 decimal places manually for accuracy)
       // profitLabel.setText("Net Profit:");
        String status = sale.getSaleNote();
        if(status.equals("sale")){
            transactionSummary.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            //txtProfit.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            transactionSummary.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
            //txtProfit.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
        //txtProfit.setText(String.format("%.2f DA", sale.getSaleProfit()));

        // Optional extension: You can display total summary contextual data here too
        // e.g., "Total: %.2f | Paid: %.2f"
    }

    /**
     * Resets the UI components safely if a user clicks empty space or clears filters.
     */
    private void clearUIFields() {
       transactionSummary.setText("No Transaction Selected");

    }

    public void loadInitialSalesTableData() {
        if (salesHeaderRepository == null) return;

        // 1. Instantiate the observable list wrapper exactly like your pattern
        this.salesList = FXCollections.observableArrayList();

        // 2. Fetch today's records by default to keep performance tight
        List<SaleHeader> sl = salesHeaderRepository.findByDate(LocalDate.now());
        this.salesList.addAll(sl);
        txtTotalSalesCount.setText(String.valueOf(salesList.size()));
        BigDecimal cashFlow = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        for(SaleHeader sh : salesList){
            cashFlow = cashFlow.add(sh.getSaleTotal());
            profit = profit.add(sh.getSaleProfit());
        }
        txtTotalCashFlow.setText(cashFlow + " DA");
        txtTheoreticalProfit.setText(profit + " DA");
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
    private void loadSalesTableByDomain(LocalDate beginning,LocalDate end){
        if (salesHeaderRepository == null) return;
            // Fetch transactions within the specified date range
        List<SaleHeader> rangeSales = salesHeaderRepository.getByRange(beginning, end);

            // Update the master list backing the TableView
        this.salesList.setAll(rangeSales);

        this.txtTotalSalesCount.setText(String.valueOf(salesList.size()));
        BigDecimal cashFlow = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        for(SaleHeader sh : salesList){
            cashFlow = cashFlow.add(sh.getSaleTotal());
            profit = profit.add(sh.getSaleProfit());
        }
        txtTotalCashFlow.setText(cashFlow + " DA");
        txtTheoreticalProfit.setText(profit + " DA");
    }
    private void setDomainSearchButton() {
        btnFilterDate.setOnAction(_ -> {
            LocalDate startDate = dpStartDate.getValue();
            LocalDate endDate = dpEndDate.getValue();

            // 1. Validation: Ensure both dates are selected
            if (startDate == null || endDate == null) {
                showAlert("Invalid Date Range", "Please select both a start date and an end date.");
                return;
            }

            // 2. Validation: Ensure correct chronological order
            if (startDate.isAfter(endDate)) {
                showAlert("Invalid Date Range", "Start date cannot be after the end date.");
                return;
            }

            // TODO: Filter salesTable list by [startDate 00:00:00] to [endDate 23:59:59]
            // TODO: Re-calculate period metrics (txtTotalSalesCount, txtTotalCashFlow, txtTheoreticalProfit)
            loadSalesTableByDomain(startDate, endDate);
        });

        btnClearFilter.setOnAction(_ -> {
            dpStartDate.setValue(null);
            dpEndDate.setValue(null);

            // TODO: Reset table filter to show all sales and recalculate default metrics
            loadInitialSalesTableData();
        });
    }

    private void setSaleDetailsButton() {
        btnOpenDetails.setOnAction(_ -> {
            // 1. Grab selected transaction header
            SaleHeader selectedSale = salesTable.getSelectionModel().getSelectedItem();

            if (selectedSale == null) {
                showAlert("No Transaction Selected", "Please select a sale from the table to view its itemized details.");
                return;
            }

            // TODO: Search / fetch the list of bought items using salesDetailRepository (e.g., repository.findBySaleId(selectedSale.getSaleId()))
            List<SaleDetailItem> boughtItems = saleDetailsRepository.getSaleDetails(selectedSale.getSaleId());

            // 2. Load and launch modal
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/SaleDetailsModalView.fxml"));
                Parent modalRoot = loader.load();

                // Pass header + bought items to the modal controller
                SaleDetailsModalController controller = loader.getController();
                controller.populateModalData(selectedSale, boughtItems);

                // Create and show modal window
                Stage modalStage = new Stage();
                modalStage.setTitle(String.format("Sale Details — Invoice #%d", selectedSale.getSaleId()));
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.initOwner(salesTable.getScene().getWindow());
                modalStage.setScene(new Scene(modalRoot));
                modalStage.setResizable(false);
                modalStage.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("UI Error", "Failed to load the invoice details modal: " + e.getMessage());
            }
        });
    }
}

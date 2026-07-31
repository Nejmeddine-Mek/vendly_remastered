package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import org.example.vendly_remastered.security.Config;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class HistoryViewController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<SaleHeader> salesTable;
    @FXML private TableColumn<SaleHeader, Integer> colSaleId;
    @FXML private TableColumn<SaleHeader, String> colDate;
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

    private boolean isOpneModal = false;

    public void setSaleDetailsRepository(ISaleDetailsRepository sdr) {
        this.saleDetailsRepository = sdr;
    }

    public void setSalesHeaderRepository(ISalesHeaderRepository salesHeaderRepository) {
        this.salesHeaderRepository = salesHeaderRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableViewBehaviour();
        setTableColumnsBehaviour();
        setSaleDetailsButton();
        setDomainSearchButton();
    }

    public void setTableColumnsBehaviour() {
        colSaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("clientName"));

        // Format Date Time clearly for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getSaleDate();
            String formatted = (date != null) ? date.format(formatter) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("saleTotal"));
        colPaid.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
    }

    public void setTableViewBehaviour() {
        salesTable.setOnMouseClicked(click -> {
            if(click.getClickCount() == 2){
                if(isOpneModal)
                    return;

                isOpneModal = true;
                btnOpenDetails.fire();
                isOpneModal = false;
            }
        });
        salesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateUIWithSelectedSale(newSelection);
            } else {
                clearUIFields();
            }
        });
    }

    private void updateUIWithSelectedSale(SaleHeader sale) {
        txtSelectedSaleId.setText(String.valueOf(sale.getSaleId()));
        txtSelectedDate.setText(sale.getSaleDate() != null ? sale.getSaleDate().toString() : "N/A");
        txtSelectedClient.setText(sale.getClientName() == null ? "N/A" : sale.getClientName());
        txtSelectedCashier.setText(sale.getSaleUsername());
        txtSelectedProfit.setText(sale.getSaleProfit() != null ? String.format("%.2f DA", sale.getSaleProfit()) : "0.00 DA");
        txtSelectedDebt.setText(sale.getRemainingDebt() != null ? String.format("%.2f DA", sale.getRemainingDebt()) : "0.00 DA");

        String note = sale.getSaleNote() != null ? sale.getSaleNote().toUpperCase() : "SALE";
        transactionSummary.setText(String.format("TX #%d — %s", sale.getSaleId(), note));


        if(sale.getSaleNote().compareToIgnoreCase("return") == 0 || sale.getSaleNote().compareToIgnoreCase("loss") == 0){
            String redStyle = "-fx-text-fill: #DC2626; -fx-font-weight: bold;";
            txtSelectedDebt.setStyle(redStyle);
            txtSelectedProfit.setStyle(redStyle);
        } else{
            String greenStyle = "-fx-text-fill: #16A34A; -fx-font-weight: bold;";

        }
        if ("SALE".equalsIgnoreCase(note)) {
            transactionSummary.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        } else {
            transactionSummary.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void clearUIFields() {
        txtSelectedSaleId.clear();
        txtSelectedDate.clear();
        txtSelectedClient.clear();
        txtSelectedCashier.clear();
        txtSelectedProfit.clear();
        txtSelectedDebt.clear();
        transactionSummary.setText("No Transaction Selected");
        transactionSummary.setStyle("-fx-text-fill: #757575; -fx-font-weight: normal;");
    }

    public void loadInitialSalesTableData() {
        if (salesHeaderRepository == null) return;

        // 1. Instantiate master list
        this.salesList = FXCollections.observableArrayList();

        // 2. Wrap in FilteredList & SortedList
        this.filteredSales = new FilteredList<>(this.salesList, p -> true);
        SortedList<SaleHeader> sortedData = new SortedList<>(this.filteredSales);
        sortedData.comparatorProperty().bind(this.salesTable.comparatorProperty());

        // 3. Set up Search Listener now that filteredSales is initialized
        setSearchBarBehaviour();

        // 4. Bind SortedList to TableView
        this.salesTable.setItems(sortedData);

        // 5. Fetch initial data (e.g. today's sales)
        List<SaleHeader> todaySales = salesHeaderRepository.findByDate(LocalDate.now());
        updateSalesData(todaySales);
    }

    public void setSearchBarBehaviour() {
        if (this.filteredSales == null || searchField == null) return;

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            this.filteredSales.setPredicate(sale -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String searchKeyword = newValue.toLowerCase().trim();

                // Match 1: Transaction ID
                if (String.valueOf(sale.getSaleId()).contains(searchKeyword)) {
                    return true;
                }

                // Match 2: Cashier Username
                if (sale.getSaleUsername() != null && sale.getSaleUsername().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                // Match 3: Client Display Name
                if (sale.getClientName() != null && sale.getClientName().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                // Match 4: Transaction Note / Type
                return sale.getSaleNote() != null && sale.getSaleNote().toLowerCase().contains(searchKeyword);
            });
        });
    }

    private void loadSalesTableByDomain(LocalDate beginning, LocalDate end) {
        if (salesHeaderRepository == null) return;

        List<SaleHeader> rangeSales = salesHeaderRepository.getByRange(beginning, end);
        updateSalesData(rangeSales);
    }

    private void setDomainSearchButton() {
        btnFilterDate.setOnAction(_ -> {
            LocalDate startDate = dpStartDate.getValue();
            LocalDate endDate = dpEndDate.getValue();

            if (startDate == null || endDate == null) {
                showAlert("Invalid Date Range", "Please select both a start date and an end date.");
                return;
            }

            if (startDate.isAfter(endDate)) {
                showAlert("Invalid Date Range", "Start date cannot be after the end date.");
                return;
            }

            loadSalesTableByDomain(startDate, endDate);
        });

        btnClearFilter.setOnAction(_ -> {
            dpStartDate.setValue(null);
            dpEndDate.setValue(null);

            loadInitialSalesTableData();
        });
    }

    private void setSaleDetailsButton() {
        btnOpenDetails.setOnAction(_ -> {
            SaleHeader selectedSale = salesTable.getSelectionModel().getSelectedItem();

            if (selectedSale == null) {
                showAlert("No Transaction Selected", "Please select a sale from the table to view its itemized details.");
                return;
            }

            List<SaleDetailItem> boughtItems = saleDetailsRepository.getSaleDetails(selectedSale.getSaleId());

            try {
                Config config = Config.getInstance();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/SaleDetailsModalView.fxml"));
                loader.setResources(config.getLanguageBundle());
                Parent modalRoot = loader.load();
                config.applyOrientation(modalRoot);
                SaleDetailsModalController controller = loader.getController();
                controller.populateModalData(selectedSale, boughtItems);

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

    private void updateSalesData(List<SaleHeader> newSales) {
        // Updating salesList automatically propagates: salesList -> filteredSales -> sortedData -> salesTable
        this.salesList.setAll(newSales);

        this.txtTotalSalesCount.setText(String.valueOf(salesList.size()));

        BigDecimal cashFlow = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        for (SaleHeader sh : salesList) {
            if (sh.getSaleTotal() != null) {
                cashFlow = cashFlow.add(sh.getSaleTotal());
            }
            if (sh.getSaleProfit() != null) {
                profit = profit.add(sh.getSaleProfit());
            }
        }

        this.txtTotalCashFlow.setText(String.format("%.2f DA", cashFlow));
        this.txtTheoreticalProfit.setText(String.format("%.2f DA", profit));
    }
}
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
import org.example.vendly_remastered.models.entity.PurchaseDetailItem;
import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.repository.IPurchaseDetailsRepository;
import org.example.vendly_remastered.repository.IPurchaseHeaderRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class PurchaseHistoryViewController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private TableView<PurchaseHeader> purchasesTable;
    @FXML private TableColumn<PurchaseHeader, Long> colPurchaseId;
    @FXML private TableColumn<PurchaseHeader, String> colDate;
    @FXML private TableColumn<PurchaseHeader, String> colSupplier;
    @FXML private TableColumn<PurchaseHeader, BigDecimal> colTotalCost;
    @FXML private TableColumn<PurchaseHeader, BigDecimal> colAmountPaid;

    @FXML private TextField txtSelectedPurchaseId;
    @FXML private TextField txtSelectedDate;
    @FXML private TextField txtSelectedSupplier;
    @FXML private TextField txtSelectedUser;
    @FXML private TextField txtSelectedTotalCost;
    @FXML private TextField txtSelectedDebt;

    @FXML private TextField txtTotalPurchasesCount;
    @FXML private TextField txtTotalProcurementCost;
    @FXML private TextField txtTotalUnpaidDebts;

    @FXML private Label transactionSummary;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button btnFilterDate;
    @FXML private Button btnClearFilter;
    @FXML private Button btnOpenDetails;
    @FXML private Button btnAddPurchase;

    private boolean isOpenModal = false;
    private IPurchaseDetailsRepository purchaseDetailsRepository;
    private IPurchaseHeaderRepository purchaseHeaderRepository;

    private ObservableList<PurchaseHeader> purchasesList;
    private FilteredList<PurchaseHeader> filteredPurchases;

    public void setPurchaseDetailsRepository(IPurchaseDetailsRepository pdr) {
        this.purchaseDetailsRepository = pdr;
    }

    public void setPurchaseHeaderRepository(IPurchaseHeaderRepository purchaseHeaderRepository) {
        this.purchaseHeaderRepository = purchaseHeaderRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableViewBehaviour();
        setTableColumnsBehaviour();
        btnAddPurchase.setOnAction(_ -> {
            openModal(null);
        });
        setPurchaseDetailsButton();

        setDomainSearchButton();
    }

    private void setTableColumnsBehaviour() {
        colPurchaseId.setCellValueFactory(new PropertyValueFactory<>("purchaseId"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        // Format Date Time clearly for display
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getPurchaseDate();
            String formatted = (date != null) ? date.format(formatter) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        colTotalCost.setCellValueFactory(new PropertyValueFactory<>("purchaseCost"));
        colAmountPaid.setCellValueFactory(new PropertyValueFactory<>("amountPaid"));
    }

    private void setTableViewBehaviour() {
        purchasesTable.setOnMouseClicked(click -> {
            if(isOpenModal)
                return;
            if(click.getClickCount() == 2){
                isOpenModal = true;
                btnOpenDetails.fire();
                isOpenModal = false;
            }
        });
        purchasesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldSelection, newSelection) -> {
            if (newSelection != null) {
                updateUIWithSelectedPurchase(newSelection);
            } else {
                clearUIFields();
            }
        });
    }

    private void updateUIWithSelectedPurchase(PurchaseHeader purchase) {
        txtSelectedPurchaseId.setText(String.valueOf(purchase.getPurchaseId()));
        txtSelectedDate.setText(purchase.getPurchaseDate() != null ? purchase.getPurchaseDate().toString() : "N/A");
        txtSelectedSupplier.setText(purchase.getSupplierName() == null ? "N/A" : purchase.getSupplierName());
        txtSelectedUser.setText(purchase.getUserName() == null ? "N/A" : purchase.getUserName());
        txtSelectedTotalCost.setText(purchase.getPurchaseCost() != null ? String.format("%.2f DA", purchase.getPurchaseCost()) : "0.00 DA");
        txtSelectedDebt.setText(purchase.getUnpaidBalance() != null ? String.format("%.2f DA", purchase.getUnpaidBalance()) : "0.00 DA");

        String supplier = purchase.getSupplierName() != null ? purchase.getSupplierName() : "UNKNOWN SUPPLIER";
        transactionSummary.setText(String.format("PO #%d — %s", purchase.getPurchaseId(), supplier));
        transactionSummary.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
    }

    private void clearUIFields() {
        txtSelectedPurchaseId.clear();
        txtSelectedDate.clear();
        txtSelectedSupplier.clear();
        txtSelectedUser.clear();
        txtSelectedTotalCost.clear();
        txtSelectedDebt.clear();
        transactionSummary.setText("No Purchase Order Selected");
        transactionSummary.setStyle("-fx-text-fill: #757575; -fx-font-weight: normal;");
    }

    public void loadInitialPurchaseTableData() {
        if (purchaseHeaderRepository == null) return;

        // 1. Instantiate master list
        this.purchasesList = FXCollections.observableArrayList();

        // 2. Wrap in FilteredList & SortedList
        this.filteredPurchases = new FilteredList<>(this.purchasesList, p -> true);
        SortedList<PurchaseHeader> sortedData = new SortedList<>(this.filteredPurchases);
        sortedData.comparatorProperty().bind(this.purchasesTable.comparatorProperty());

        // 3. Set up Search Listener now that filteredPurchases is initialized
        setSearchBarBehaviour();

        // 4. Bind SortedList to TableView
        this.purchasesTable.setItems(sortedData);

        // 5. Fetch initial data (today's purchase orders)
        List<PurchaseHeader> todayPurchases = purchaseHeaderRepository.findByDate(LocalDate.now());
        updatePurchasesData(todayPurchases);
    }

    public void setSearchBarBehaviour() {
        if (this.filteredPurchases == null || searchField == null) return;

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            this.filteredPurchases.setPredicate(purchase -> {
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String searchKeyword = newValue.toLowerCase().trim();

                // Match 1: Purchase Order ID
                if (String.valueOf(purchase.getPurchaseId()).contains(searchKeyword)) {
                    return true;
                }

                // Match 2: Supplier Name
                if (purchase.getSupplierName() != null && purchase.getSupplierName().toLowerCase().contains(searchKeyword)) {
                    return true;
                }

                // Match 3: Purchaser / User Name
                return purchase.getUserName() != null && purchase.getUserName().toLowerCase().contains(searchKeyword);
            });
        });
    }

    private void loadPurchaseTableByDomain(LocalDate beginning, LocalDate end) {
        if (purchaseHeaderRepository == null) return;

        List<PurchaseHeader> rangePurchases = purchaseHeaderRepository.getByRange(beginning, end);
        updatePurchasesData(rangePurchases);
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

            loadPurchaseTableByDomain(startDate, endDate);
        });

        btnClearFilter.setOnAction(_ -> {
            dpStartDate.setValue(null);
            dpEndDate.setValue(null);

            loadInitialPurchaseTableData();
        });
    }

    private void setPurchaseDetailsButton() {
        btnOpenDetails.setOnAction(_ -> {
            PurchaseHeader selectedPurchase = purchasesTable.getSelectionModel().getSelectedItem();

            if (selectedPurchase == null) {
                showAlert("No Purchase Selected", "Please select a purchase order from the table to view its items.");
                return;
            }
            openModal(selectedPurchase);
        });
    }

    private void openModal(PurchaseHeader selectedPurchase){
        List<PurchaseDetailItem> boughtItems = new ArrayList<>();
        if(selectedPurchase != null)
            boughtItems = purchaseDetailsRepository.getPurchaseDetails(selectedPurchase.getPurchaseId());

        try {
            Config config = Config.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/PurchaseDetailsModal.fxml"));
            loader.setResources(config.getLanguageBundle());
            Parent modalRoot = loader.load();
            config.applyOrientation(modalRoot);

            PurchaseEntryViewController controller = loader.getController();
            controller.populateModalData(selectedPurchase, boughtItems);

            ControllerDispatcher.execute(controller);

            Stage modalStage = new Stage();
            if(selectedPurchase == null){
                modalStage.setTitle(String.format("register New Order"));
            }
            else
                modalStage.setTitle(String.format("Purchase Order Details — #%d", selectedPurchase.getPurchaseId()));

            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.initOwner(purchasesTable.getScene().getWindow());
            modalStage.setScene(new Scene(modalRoot));
            modalStage.setResizable(false);
            modalStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("UI Error", "Failed to load the purchase order details modal: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void updatePurchasesData(List<PurchaseHeader> newPurchases) {
        // Updating purchasesList automatically propagates: purchasesList -> filteredPurchases -> sortedData -> purchasesTable
        this.purchasesList.setAll(newPurchases);

        this.txtTotalPurchasesCount.setText(String.valueOf(purchasesList.size()));

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalUnpaid = BigDecimal.ZERO;

        for (PurchaseHeader ph : purchasesList) {
            if (ph.getPurchaseCost() != null) {
                totalCost = totalCost.add(ph.getPurchaseCost());
            }
            if (ph.getUnpaidBalance() != null) {
                totalUnpaid = totalUnpaid.add(ph.getUnpaidBalance());
            }
        }

        this.txtTotalProcurementCost.setText(String.format("%.2f DA", totalCost));
        this.txtTotalUnpaidDebts.setText(String.format("%.2f DA", totalUnpaid));
    }


}
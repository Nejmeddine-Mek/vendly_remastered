package org.example.vendly_remastered.controls;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.example.vendly_remastered.models.entity.PurchaseHeader;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PurchaseHistoryViewController implements Initializable {
    @FXML private Label pageTitle;
    @FXML private TextField searchField;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Button btnFilterDate;
    @FXML private Button btnClearFilter;

    @FXML private TableView<PurchaseHeader> purchasesTable;
    @FXML private TableColumn<PurchaseHeader, Long> colPurchaseId;
    @FXML private TableColumn<PurchaseHeader, String> colDate;
    @FXML private TableColumn<PurchaseHeader, String> colSupplier;
    @FXML private TableColumn<PurchaseHeader, BigDecimal> colTotalCost;
    @FXML private TableColumn<PurchaseHeader, BigDecimal> colAmountPaid;

    @FXML private TextField txtTotalPurchasesCount;
    @FXML private TextField txtTotalProcurementCost;
    @FXML private TextField txtTotalUnpaidDebts;

    @FXML private TextField txtSelectedPurchaseId;
    @FXML private TextField txtSelectedDate;
    @FXML private TextField txtSelectedSupplier;
    @FXML private TextField txtSelectedUser;
    @FXML private TextField txtSelectedTotalCost;
    @FXML private TextField txtSelectedDebt;
    @FXML private Button btnOpenDetails;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
    }
    private void setTableColumnsBehaviour() {
        // 1. Basic ID Column
        colPurchaseId.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPurchaseId())
        );

        // 2. Formatted Date & Time Column (e.g. "2026-07-24 17:45")
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getPurchaseDate();
            String formatted = (date != null) ? date.format(dateFormatter) : "N/A";
            return new SimpleStringProperty(formatted);
        });

        // 3. Joined Supplier Name (Fallback to ID if name is null)
        colSupplier.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getSupplierName();
            if (name == null || name.isBlank()) {
                name = "Supplier #" + cellData.getValue().getSupplierId();
            }
            return new SimpleStringProperty(name);
        });

        // 4. Formatted Total Cost Column
        colTotalCost.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getPurchaseCost())
        );
        colTotalCost.setCellFactory(column -> new TableCell<PurchaseHeader, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DZD", item));
                }
            }
        });

        // 5. Formatted Amount Paid Column
        colAmountPaid.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAmountPaid())
        );
        colAmountPaid.setCellFactory(column -> new TableCell<PurchaseHeader, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DZD", item));
                }
            }
        });
    }

}

package org.example.vendly_remastered.controls;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.security.Config;

import java.math.BigDecimal;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class SaleDetailsModalController implements Initializable {

    // Header Fields
    @FXML private Label modalTitle;
    @FXML private TextField txtHeaderSaleId;
    @FXML private TextField txtHeaderDate;
    @FXML private TextField txtHeaderClient;
    @FXML private TextField txtHeaderCashier;
    @FXML private TextField txtHeaderTotal;

    // Items Table
    @FXML private TableView<SaleDetailItem> itemsTable;
    @FXML private TableColumn<SaleDetailItem, Integer> colProductId;
    @FXML private TableColumn<SaleDetailItem, String> colProductName;
    @FXML private TableColumn<SaleDetailItem, BigDecimal> colQuantity;
    @FXML private TableColumn<SaleDetailItem, BigDecimal> colUnitPrice;
    @FXML private TableColumn<SaleDetailItem, BigDecimal> colLineTotal;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumns();
    }

    private void setTableColumns() {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("productQuantity"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("productUnitPrice"));

        // Calculated Subtotal Column: quantity * unitPrice
        colLineTotal.setCellValueFactory(cellData -> {
            SaleDetailItem item = cellData.getValue();
            if (item != null && item.getProductQuantity() != null && item.getProductUnitPrice() != null) {
                return new SimpleObjectProperty<>(item.getProductQuantity().multiply(item.getProductUnitPrice()));
            }
            return new SimpleObjectProperty<>(BigDecimal.ZERO);
        });
    }

    /**
     * Call this method from HistoryViewController right after loading the FXML modal window.
     */
    public void populateModalData(SaleHeader sale, List<SaleDetailItem> items) {
        if (sale != null) {
            var bundle = Config.getInstance().getLanguageBundle();
            String title = MessageFormat.format(
                    bundle.getString("sales.invoiceLineItemsTitle"),
                    sale.getSaleId()
            );

            modalTitle.setText(title);
            txtHeaderSaleId.setText("#" + sale.getSaleId());
            txtHeaderDate.setText(String.valueOf(sale.getSaleDate()));
            txtHeaderClient.setText(sale.getClientName() == null || sale.getClientName().isBlank() ? Config.getInstance().getLanguageBundle().getString("customer.walkIn") : sale.getClientName());
            txtHeaderCashier.setText(sale.getSaleUsername());
            txtHeaderTotal.setText(String.format("%.2f DA", sale.getSaleTotal() != null ? sale.getSaleTotal() : BigDecimal.ZERO));

            if (sale.getSaleNote() != null && (sale.getSaleNote().trim().equalsIgnoreCase("return") || sale.getSaleNote().trim().equalsIgnoreCase("loss"))) {
                String redStyle = "-fx-text-fill: #DC2626 !important; -fx-font-weight: bold;";
                txtHeaderTotal.setStyle(redStyle);
                modalTitle.setStyle(redStyle);
                txtHeaderSaleId.setStyle(redStyle);
            }

        }

        if (items != null) {
            itemsTable.setItems(FXCollections.observableArrayList(items));
        }
    }
}
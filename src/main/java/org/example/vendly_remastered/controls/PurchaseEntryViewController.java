package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.models.entity.PurchaseDetailItem;
import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.models.entity.Supplier;

import java.math.BigDecimal;
import java.util.List;

public class PurchaseEntryViewController {
    // ==========================================
    // 1. HEADER & METADATA
    // ==========================================
    @FXML private Label titleLabel;
    @FXML private Label lblOrderMode;
    @FXML private TextField txtPurchaseId;
    @FXML private TextField txtInvoiceNumber;
    @FXML private ComboBox<Supplier> cbSupplier; // Replace Supplier with your model
    @FXML private DatePicker dpPurchaseDate;
    @FXML private TextField txtCreatedBy;

    // ==========================================
    // 2. FISCAL & LEGAL NUMBERS
    // ==========================================
    @FXML private TextField txtRC;
    @FXML private TextField txtNIF;
    @FXML private TextField txtNIS;
    @FXML private TextField txtART;
    @FXML private TextField txtCPA;

    // ==========================================
    // 3. ROW ENTRY BAR
    // ==========================================
    @FXML private HBox hboxEntryBar;
    @FXML private ComboBox<Product> cbProduct; // Replace Product with your model
    @FXML private TextField txtUnitCost;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtLineSubtotal;
    @FXML private Button btnAddRow;

    // ==========================================
    // 4. TABLE VIEW & COLUMNS
    // ==========================================
    @FXML private TableView<PurchaseDetailItem> tablePurchaseDetails; // Replace with your model
    @FXML private TableColumn<PurchaseDetailItem, Integer> colItemNo;
    @FXML private TableColumn<PurchaseDetailItem, String> colProductName;
    @FXML private TableColumn<PurchaseDetailItem, BigDecimal> colUnitCost;
    @FXML private TableColumn<PurchaseDetailItem, Integer> colQuantity;
    @FXML private TableColumn<PurchaseDetailItem, BigDecimal> colSubtotal;
    @FXML private TableColumn<PurchaseDetailItem, Void> colAction;

    // ==========================================
    // 5. FISCAL TOTALS & FOOTER
    // ==========================================
    @FXML private TextField txtMontantHT;
    @FXML private TextField txtRemise;
    @FXML private ComboBox<String> cbTvaRate; // e.g., "19%", "9%", "0%"
    @FXML private TextField txtMontantTVA;
    @FXML private TextField txtTimbre;
    @FXML private TextField txtNetToPay;
    @FXML private TextField txtAmountPaid;
    @FXML private TextField txtRemainingDebt;
    @FXML private Button btnSavePurchase;
    public void setReadOnlyMode(boolean readOnly) {
        // 1. Hide/Show Entry elements
        hboxEntryBar.setVisible(!readOnly);
        hboxEntryBar.setManaged(!readOnly);
        colAction.setVisible(!readOnly);
        btnSavePurchase.setVisible(!readOnly);
        btnSavePurchase.setManaged(!readOnly);

        // 2. Lock Header / Supplier fields
        cbSupplier.setDisable(readOnly);
        dpPurchaseDate.setDisable(readOnly);
        txtInvoiceNumber.setEditable(!readOnly);
        txtRC.setEditable(!readOnly);
        txtNIF.setEditable(!readOnly);
        txtNIS.setEditable(!readOnly);
        txtART.setEditable(!readOnly);
        txtCPA.setEditable(!readOnly);

        // 3. Lock Tax Breakdown Inputs
        txtRemise.setEditable(!readOnly);
        cbTvaRate.setDisable(readOnly);
        txtTimbre.setEditable(!readOnly);
        txtAmountPaid.setEditable(!readOnly);

        // 4. Update Header Indicator
        if (readOnly) {
            lblOrderMode.setText("READ ONLY");
            lblOrderMode.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold;");
        }
    }

    public void populateModalData(PurchaseHeader selectedPurchase, List<PurchaseDetailItem> boughtItems) {
    }
}

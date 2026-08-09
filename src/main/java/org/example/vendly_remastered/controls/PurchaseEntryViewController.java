package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.vendly_remastered.models.entity.*;
import org.example.vendly_remastered.repository.*;

import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.security.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class PurchaseEntryViewController implements Initializable {
    // ==========================================
    // 1. HEADER & METADATA
    // ==========================================

    @FXML private Label lblOrderMode;
    @FXML private TextField txtPurchaseId;
    @FXML private TextField txtInvoiceNumber;
    @FXML private ComboBox<Supplier> cbSupplier; // Replace Supplier with your model
    @FXML private DatePicker dpPurchaseDate;

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
    @FXML private TableColumn<PurchaseDetailItem,LocalDate> colExpiryDate;
    @FXML private TableColumn<PurchaseDetailItem,String> colBatchNumber;
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

    private final ObservableList<PurchaseDetailItem> detailItems = FXCollections.observableArrayList();

    private IPurchaseHeaderRepository purchaseHeaderRepository;
    private IPurchaseDetailsRepository purchaseDetailRepository;
    private IBatchRepository batchRepository;
    private IProductRepository productRepository;
    private ISuppliersRepository suppliersRepository;
    private IStockRepository stockRepository;


    public void setStockRepository(IStockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public void setBatchRepository(IBatchRepository batchRepository){
        this.batchRepository = batchRepository;
    }
    public void setProductRepository(IProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void setSuppliersRepository(ISuppliersRepository suppliersRepository) {
        this.suppliersRepository = suppliersRepository;
    }


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
        if (selectedPurchase != null) {
            setReadOnlyMode(true);

            // 1. Header & ID Info
            if (txtPurchaseId != null) txtPurchaseId.setText(String.valueOf(selectedPurchase.getPurchaseId()));
            txtInvoiceNumber.setText(selectedPurchase.getInvoiceNumber());
            dpPurchaseDate.setValue(selectedPurchase.getPurchaseDate() != null ? selectedPurchase.getPurchaseDate().toLocalDate() : null);

            // Match and set Supplier
            if (cbSupplier.getItems() != null) {
                cbSupplier.getItems().stream()
                        .filter(s -> s.getSupplierId() == selectedPurchase.getSupplierId())
                        .findFirst()
                        .ifPresent(cbSupplier::setValue);
            }

            // 2. Fiscal & Legal Numbers
            txtRC.setText(selectedPurchase.getRcNumber());
            txtNIF.setText(selectedPurchase.getNifNumber());
            txtNIS.setText(selectedPurchase.getNisNumber());
            txtART.setText(selectedPurchase.getArtNumber());
            txtCPA.setText(selectedPurchase.getCpaAccount());

            // 3. Fiscal Totals & Footer Fields
            txtMontantHT.setText(selectedPurchase.getTotalHt() != null ? selectedPurchase.getTotalHt().toPlainString() : "0.00");
            txtRemise.setText(selectedPurchase.getDiscountAmount() != null ? selectedPurchase.getDiscountAmount().toPlainString() : "0.00");

            if (selectedPurchase.getTvaRate() != null) {
                cbTvaRate.setValue(selectedPurchase.getTvaRate().toPlainString() + "%");
            }

            txtMontantTVA.setText(selectedPurchase.getTvaAmount() != null ? selectedPurchase.getTvaAmount().toPlainString() : "0.00");
            txtTimbre.setText(selectedPurchase.getStampDuty() != null ? selectedPurchase.getStampDuty().toPlainString() : "0.00");
            txtNetToPay.setText(selectedPurchase.getPurchaseCost() != null ? selectedPurchase.getPurchaseCost().toPlainString() : "0.00");
            txtAmountPaid.setText(selectedPurchase.getAmountPaid() != null ? selectedPurchase.getAmountPaid().toPlainString() : "0.00");
            txtNetToPay.setText(String.valueOf(selectedPurchase.getPurchaseCost()));
            // Calculate remaining debt based on NetToPay and AmountPaid
            BigDecimal net = selectedPurchase.getPurchaseCost() != null ? selectedPurchase.getPurchaseCost() : BigDecimal.ZERO;
            BigDecimal paid = selectedPurchase.getAmountPaid() != null ? selectedPurchase.getAmountPaid() : BigDecimal.ZERO;
            txtRemainingDebt.setText(net.subtract(paid).setScale(2, RoundingMode.HALF_UP).toPlainString());

            // 4. Populate Table Items
            if (boughtItems != null && !boughtItems.isEmpty()) {
                detailItems.setAll(boughtItems);
                reindexTableItems();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if(!Config.getInstance().isTrackSupplierDebts()){
            txtAmountPaid.setEditable(false);
            txtRemainingDebt.setEditable(false);
            txtAmountPaid.textProperty().bind(txtNetToPay.textProperty());
            txtRemainingDebt.setText("0.0");
        }

        setupTableBindings();
        setupFieldListeners();
        cbTvaRate.getItems().addAll("0%","9%", "19%");
        btnAddRow.setOnAction(_ -> {
            handleAddRow();
        });
        btnSavePurchase.setOnAction(_ -> {
            handleSavePurchase();
        });
    }

    public void initializeComboBoxes() {
        List<Product> listOfProducts = productRepository.findAll();
        List<Supplier> listOfSuppliers = suppliersRepository.findAll();

        // 1. Fill cbProduct & set display converter
        cbProduct.setItems(FXCollections.observableArrayList(listOfProducts));
        cbProduct.setConverter(new StringConverter<Product>() {
            @Override
            public String toString(Product product) {
                return (product != null) ? product.getProductName() : "";
            }

            @Override
            public Product fromString(String string) {
                return null; // Selection-only, conversion back is unnecessary
            }
        });

        // 2. Fill cbSupplier & set display converter
        cbSupplier.setItems(FXCollections.observableArrayList(listOfSuppliers));
        cbSupplier.setConverter(new StringConverter<Supplier>() {
            @Override
            public String toString(Supplier supplier) {
                return (supplier != null) ? supplier.getSupplierFullName() : "";
            }

            @Override
            public Supplier fromString(String string) {
                return null;
            }
        });
    }

    private void setupTableBindings() {
        tablePurchaseDetails.setItems(detailItems);

        colItemNo.setCellValueFactory(new PropertyValueFactory<>("itemNumber"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colUnitCost.setCellValueFactory(new PropertyValueFactory<>("unitCost"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // BATCH & EXPIRY
        colBatchNumber.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        colExpiryDate.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button("X");
            {
                btnDelete.setOnAction(e -> {
                    PurchaseDetailItem item = getTableView().getItems().get(getIndex());
                    detailItems.remove(item);
                    reindexTableItems();
                    recalculateTotals();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    // ==========================================
    // LISTENERS & AUTO-FILL BEHAVIOR
    // ==========================================
    private void setupFieldListeners() {

        // Product Selection -> Default Unit Cost
        cbProduct.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null && selected.getCostPrice() != null) {
                txtUnitCost.setText(selected.getCostPrice().toPlainString());
                calculateLineSubtotal();
            }
        });

        // Entry Line Live Calculation
        txtQuantity.textProperty().addListener((obs, o, n) -> calculateLineSubtotal());
        txtUnitCost.textProperty().addListener((obs, o, n) -> calculateLineSubtotal());

        // Fiscal Totals Recalculation
        txtRemise.textProperty().addListener((obs, o, n) -> recalculateTotals());
        cbTvaRate.valueProperty().addListener((obs, o, n) -> recalculateTotals());
        txtTimbre.textProperty().addListener((obs, o, n) -> recalculateTotals());
        txtAmountPaid.textProperty().addListener((obs, o, n) -> recalculateTotals());
    }

    private void calculateLineSubtotal() {
        try {
            BigDecimal cost = parseSafe(txtUnitCost.getText());
            BigDecimal qty = parseSafe(txtQuantity.getText());
            txtLineSubtotal.setText(cost.multiply(qty).setScale(2, RoundingMode.HALF_UP).toString());
        } catch (Exception e) {
            txtLineSubtotal.setText("0.00");
        }
    }

    // ==========================================
    // ROW ADDITION & TABLE MANAGEMENT
    // ==========================================
    @FXML
    private void handleAddRow() {
        Product selectedProduct = cbProduct.getValue();
        if (selectedProduct == null) return;

        BigDecimal unitCost = parseSafe(txtUnitCost.getText());
        BigDecimal quantity = parseSafe(txtQuantity.getText());

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) return;

        PurchaseDetailItem item = new PurchaseDetailItem(
                detailItems.size() + 1,
                selectedProduct.getProductId(),
                selectedProduct.getProductName(),
                unitCost,
                quantity
        );

        detailItems.add(item);
        clearEntryBar();
        recalculateTotals();
        tablePurchaseDetails.refresh();
    }

    private void reindexTableItems() {
        for (int i = 0; i < detailItems.size(); i++) {
            detailItems.get(i).setItemNumber(i + 1);
        }
        tablePurchaseDetails.refresh();
    }

    private void clearEntryBar() {
        cbProduct.setValue(null);
        txtUnitCost.clear();
        txtQuantity.clear();
        txtLineSubtotal.clear();
    }

    // ==========================================
    // FISCAL CALCULATIONS (HT, TVA, Net to Pay, Debt)
    // ==========================================
    public void recalculateTotals() {
        // 1. Total HT
        BigDecimal totalHT = detailItems.stream()
                .map(PurchaseDetailItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        txtMontantHT.setText(totalHT.setScale(2, RoundingMode.HALF_UP).toString());

        // 2. Discount / Remise
        BigDecimal remise = parseSafe(txtRemise.getText());
        BigDecimal htAfterRemise = totalHT.subtract(remise).max(BigDecimal.ZERO);

        // 3. TVA Amount
        String rawTva = cbTvaRate.getValue() != null ? cbTvaRate.getValue().replace("%", "") : "0";
        BigDecimal tvaRate = parseSafe(rawTva).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        BigDecimal tvaAmount = htAfterRemise.multiply(tvaRate).setScale(2, RoundingMode.HALF_UP);
        txtMontantTVA.setText(tvaAmount.toString());

        // 4. Timbre & Purchase Cost (Net to Pay)
        BigDecimal timbre = parseSafe(txtTimbre.getText());
        BigDecimal netToPay = htAfterRemise.add(tvaAmount).add(timbre).setScale(2, RoundingMode.HALF_UP);
        txtNetToPay.setText(netToPay.toString());

        // 5. Debt
        BigDecimal amountPaid = parseSafe(txtAmountPaid.getText());
        BigDecimal remainingDebt = netToPay.subtract(amountPaid).setScale(2, RoundingMode.HALF_UP);
        txtRemainingDebt.setText(remainingDebt.toString());
    }

    // ==========================================
    // SAVE PURCHASE ACTION
    // ==========================================
    @FXML
    private void handleSavePurchase() {
        Supplier supplier = cbSupplier.getValue();
        if (supplier == null || detailItems.isEmpty()) {
            return; // Requires at least a supplier and one line item
        }

        PurchaseHeader header = new PurchaseHeader();
        header.setSupplierId(supplier.getSupplierId());
        header.setUserId((long) UserSession.getInstance().getUserId());
        header.setInvoiceNumber(txtInvoiceNumber.getText() != null ? txtInvoiceNumber.getText().trim() : "");

        // Fiscal Snapshot
        header.setRcNumber(txtRC.getText());
        header.setNifNumber(txtNIF.getText());
        header.setNisNumber(txtNIS.getText());
        header.setArtNumber(txtART.getText());
        header.setCpaAccount(txtCPA.getText());

        // Financials
        header.setTotalHt(parseSafe(txtMontantHT.getText()));
        header.setDiscountAmount(parseSafe(txtRemise.getText()));

        String rawTva = cbTvaRate.getValue() != null ? cbTvaRate.getValue().replace("%", "") : "0";
        header.setTvaRate(parseSafe(rawTva));
        header.setTvaAmount(parseSafe(txtMontantTVA.getText()));
        header.setStampDuty(parseSafe(txtTimbre.getText()));
        header.setPurchaseCost(parseSafe(txtNetToPay.getText()));
        header.setAmountPaid(parseSafe(txtAmountPaid.getText()));

        // Date Handling
        LocalDate pickedDate = dpPurchaseDate.getValue();
        header.setPurchaseDate(pickedDate != null ? pickedDate.atStartOfDay() : LocalDateTime.now());

        if (purchaseHeaderRepository != null) {
            // Get List of Expiry dates
            int generatedId = purchaseHeaderRepository.savePurchaseHeader(header);
            if (generatedId > 0) {
                List<Batch> batchList = new ArrayList<>();
                Map<Integer, BigDecimal> newItemsInStock = new HashMap<>();
                for(PurchaseDetailItem item: detailItems){

                    item.setPurchaseId((long) generatedId);

                    if (item.getQuantity() != null) {
                        newItemsInStock.put(item.getProductId(), item.getQuantity().negate());
                    }
                    if (item.getExpiryDate() != null) {
                        batchList.add(new Batch(
                                Math.toIntExact(item.getProductId()),
                                item.getProductName(),
                                item.getBatchNumber(),
                                item.getQuantity(),
                                item.getExpiryDate()
                        ));
                    }

                }

                // Next step: iterate detailItems and persist to purchase_details
                // and stock/batch schemas using generatedId
                batchRepository.saveAll(batchList);
                purchaseDetailRepository.savePurchaseItems(detailItems);
                supplier.setDebts(supplier.getDebts().add(parseSafe(txtRemainingDebt.getText())));
                if(Config.getInstance().isTrackSupplierDebts())
                    suppliersRepository.updateSupplier(supplier);
                if(Config.getInstance().isTrackStock())
                    stockRepository.updateStockBatch(newItemsInStock);
            }
        }
        closeModal();
    }

    private void closeModal() {
        if (btnSavePurchase != null && btnSavePurchase.getScene() != null) {
            Stage stage = (Stage) btnSavePurchase.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private BigDecimal parseSafe(String text) {
        if (text == null || text.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public void setPurchaseHeaderRepository(IPurchaseHeaderRepository purchaseHeaderRepository) {
        this.purchaseHeaderRepository = purchaseHeaderRepository;
    }

    public void setPurchaseDetailRepository(IPurchaseDetailsRepository purchaseDetailRepository) {
        this.purchaseDetailRepository = purchaseDetailRepository;
    }

}

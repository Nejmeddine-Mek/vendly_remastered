package org.example.vendly_remastered.controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import org.example.vendly_remastered.cache.ProductsCache;
import org.example.vendly_remastered.models.dto.CartItem;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.security.Key;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SalesViewController implements Initializable {
    @FXML private Label quantityLabel;
    @FXML private TextField quantityField;
    @FXML private Label priceLabel;
    @FXML private TextField priceField;
    @FXML private Label barcodeLabel;
    @FXML private TextField barcodeField;
    @FXML private Label noteLabel;
    @FXML private CheckBox clientCheckBox;
    @FXML private Label clientName;
    @FXML private TextField clientNameField;
    @FXML private Label paidAmountLabel;
    @FXML private TextField paidAmountField;

    // those are to fix later
    @FXML private TableView<CartItem> saleItemsTable;
    @FXML private TableColumn<CartItem, String> colBarcode;
    @FXML private TableColumn<CartItem, String> colProduct;
    @FXML private TableColumn<CartItem, BigDecimal> colQuantity;
    @FXML private TableColumn<CartItem, BigDecimal> colUnitPrice;
    @FXML private TableColumn<CartItem, BigDecimal> colSubTotal;


    @FXML private Label transactionSummaryLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label totalLabel;
    @FXML private Button confirmButton;

    private ContextMenu suggestionPopup = new ContextMenu();
    ObservableList<CartItem> saleItems;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // language file first
        //set default behaviour of fields

        setQuantityBehaviour();
        setPriceBehaviour(0.0);
        setDefaultClientFields();
        setClientCheckBoxBehaviour();
        setTextFormatting();
        setTableBehaviour();
        setColumnsBehaviour();
    }


    // let us define the behaviour of each field in a separate private method
    private void setQuantityBehaviour(){
        // we must create a formatter for this to accept only numeric values
        quantityField.setText("1.00");
        quantityField.setEditable(false);

        quantityField.setOnKeyPressed(key -> {
            if (quantityField.isFocused() && key.isAltDown()) {
                commitQuantityAndReset();
                quantityField.setEditable(false);
            }
        });
        quantityField.setOnAction(event -> {
            commitQuantityAndReset();
            quantityField.setEditable(false);
        });
        // Basically this works as follows
        // read a product then press enter
        // focus on the quantity field, we add it after all defaults are ready
    }
    private void commitQuantityAndReset(){
        String rawText = quantityField.getText();
        BigDecimal validatedQty;
        if (rawText == null || rawText.trim().isEmpty() || rawText.trim().equals(".")) {
            validatedQty = new BigDecimal("1.00");
        } else {
            try {
                validatedQty = new BigDecimal(rawText.trim());
                if (validatedQty.compareTo(BigDecimal.ZERO) <= 0) {
                    validatedQty = new BigDecimal("1.00"); // Don't allow 0 or negative quantities
                }
            } catch (NumberFormatException e) {
                validatedQty = new BigDecimal("1.00");
            }
        }
        quantityField.setText(String.format("%.2f", validatedQty));

        CartItem selectedItem = saleItemsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            selectedItem.setQuantity(validatedQty);
            // Explicitly refresh the table viewport to force recalculations if needed
            saleItemsTable.refresh();
            updateTransactionTotal(); // Recalculate full order totals
        }

        // 4. Reset states and return focus back to the workflow's starting gate
        quantityField.setEditable(false);
        barcodeField.requestFocus();
        barcodeField.selectAll();
    }
    private void updateTransactionTotal() {
        BigDecimal total = saleItems.stream()
                .map(CartItem::getSubtotal) // Assuming getSubtotal() returns a BigDecimal property value
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalAmountLabel.setText(String.format("%.2f DA", total));
    }

    private void setPriceBehaviour(double price){
        priceField.setEditable(false);
        priceField.setText(String.valueOf(price));
        setBarcodeFieldBehaviour();
    }

    private void setClientCheckBoxBehaviour(){
        clientCheckBox.selectedProperty().addListener((_, _, newValue) ->{
            if(newValue){
                clientNameField.setVisible(true);
                clientNameField.setManaged(true);
                clientName.setVisible(true);
                paidAmountField.setVisible(true);
                paidAmountField.setManaged(true);
                paidAmountLabel.setVisible(true);
            } else {
                clientNameField.setVisible(false);
                clientNameField.setManaged(false);
                clientName.setVisible(false);
                paidAmountField.setVisible(false);
                paidAmountField.setManaged(false);
                paidAmountLabel.setVisible(false);
            }
        });
    }

    private void setDefaultClientFields(){
        clientNameField.setPromptText("Client Name...");
        clientNameField.setVisible(false);
        clientNameField.setManaged(false);
        clientName.setVisible(false);
        paidAmountField.setPromptText("Paid Amount here...");
        paidAmountField.setVisible(false);
        paidAmountField.setManaged(false);
        paidAmountLabel.setVisible(false);
    }

    private void setTextFormatting(){
        quantityField.setTextFormatter(TextFormatterUtils.createDecimalFormatter());
        priceField.setTextFormatter(TextFormatterUtils.createDecimalFormatter());
        paidAmountField.setTextFormatter(TextFormatterUtils.createDecimalFormatter());
    }

    private void setTableBehaviour(){
        saleItems = FXCollections.observableArrayList();
        saleItemsTable.setItems(saleItems);
    }
    private void setColumnsBehaviour(){
        colBarcode.setCellValueFactory(cellData -> cellData.getValue().barCodeProperty());
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colQuantity.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
        colUnitPrice.setCellValueFactory(cellData -> cellData.getValue().unitPriceProperty());
        colSubTotal.setCellValueFactory(cellData -> cellData.getValue().subtotalProperty());
    }
    private void setBarcodeFieldBehaviour(){
        barcodeField.requestFocus();
        // first of all, we want to allow switching focus through clicking ALT
        barcodeField.setOnKeyPressed(key -> {
            if(barcodeField.isFocused())
                if(key.isAltDown()){
                    quantityField.setEditable(true);
                    quantityField.requestFocus();
                }
        });
// 1. Text Listener to intercept typing for '@' search suggestions
        barcodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || !newVal.startsWith("@")) {
                suggestionPopup.hide();
                return;
            }

            String query = newVal.substring(1).toLowerCase().trim();

            // Fetch matching items from cache or repository index
            ProductsCache cache = ProductsCache.getInstance();
            // Assuming your cache or masterData can give you filtered results:
            List<Product> matches = cache.searchByName(query);

            if (matches.isEmpty()) {
                suggestionPopup.hide();
                return;
            }

            // Rebuild context menu items dynamically
            suggestionPopup.getItems().clear();
            for (Product prod : matches) {
                MenuItem item = new MenuItem(prod.getProductName());
                item.setOnAction(e -> {
                    // When clicked, clear the text field and simulate an explicit barcode fetch
                    //barcodeField.setText(prod.getBarcode()); // or pass product directly to your cart adder
                    barcodeField.setText("@" + item.getText());
                    suggestionPopup.hide();
                    barcodeField.fireEvent(new javafx.event.ActionEvent()); // Trigger ActionEvent
                });
                suggestionPopup.getItems().add(item);
            }

            // Show the dropdown right under the barcode field
            if (!suggestionPopup.isShowing()) {
                suggestionPopup.show(barcodeField, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });
        barcodeField.setOnAction(event -> {
            String inputBarcode = barcodeField.getText();
            if (inputBarcode == null || inputBarcode.trim().isEmpty()) {
                return;
            }
            String targetBarcode = inputBarcode.trim();
            ProductsCache cache = ProductsCache.getInstance();
            Product targetProduct;
            // the case of searching by name
            if(targetBarcode.charAt(0) == '@'){
                System.out.println("looking for product: " + targetBarcode.replace("@","").toLowerCase());
                targetProduct = cache.getByName(targetBarcode.replace("@","").toLowerCase());

            } else {
                targetProduct = cache.getByBarcode(targetBarcode);
            }
            if(targetProduct == null){
                System.out.println("Product with barcode or Name " + targetBarcode + " was not found");
                return;
            }// 2. Check if the product is already inside your cart list
            Optional<CartItem> existingItemOpt = saleItems.stream()
                    .filter(item -> item.getProductId() == targetProduct.getProductId())
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                // Product exists! Grab it and increment its quantity property
                CartItem existingItem = existingItemOpt.get();
                BigDecimal currentQty = existingItem.getQuantity(); // Assuming getQuantity() returns BigDecimal
                existingItem.setQuantity(currentQty.add(BigDecimal.ONE));

                // Note: Ensure your CartItem's subtotalProperty updates automatically
                // when quantity or unitPrice changes so the UI refreshes instantly!
            } else {
                // Product is new! Instantiate a fresh row entry
                CartItem newItem = new CartItem(
                        targetProduct.getProductId(),
                        targetBarcode,
                        targetProduct.getProductName(),
                        targetProduct.getRetailPrice(),
                        BigDecimal.ONE
                );
                saleItems.add(newItem);
            }

        });
    }
    // from now on, the rest will involve searching and backend related tasks, we stop here for now
}

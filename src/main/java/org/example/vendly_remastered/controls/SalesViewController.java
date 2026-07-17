package org.example.vendly_remastered.controls;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.cache.ProductsCache;
import org.example.vendly_remastered.models.dto.CartItem;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISaleDetailsRepository;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.service.AuditLogsService;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final ContextMenu suggestionPopup = new ContextMenu();
    ObservableList<CartItem> saleItems;

    // A thread pool with 2 dedicated worker threads for background operations
    private final ExecutorService salesThreadPool = Executors.newFixedThreadPool(2);
    ISalesHeaderRepository saleHeaderRepository;
    ISaleDetailsRepository saleDetailsRepository;

    public void setSaleDetailsRepository(ISaleDetailsRepository saleDetailsRepository){
        this.saleDetailsRepository = saleDetailsRepository;
    }

    public void setSaleHeaderRepository(ISalesHeaderRepository saleHeaderrepository) {
        this.saleHeaderRepository = saleHeaderrepository;
    }

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
        setConfirmButtonBehaviour();
        setClientNameFieldBehaviour();
    }

    private void setConfirmButtonBehaviour(){
        confirmButton.setOnAction(_ -> {
            saveSale();
        });
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

        totalLabel.setText(String.format("%.2f DA", total));
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
                if (targetProduct != null && targetProduct.getProductName().equals("rollbackState")) {

                    // 1. Force the UI to pop open the suggestions menu again
                    triggerSuggestionsDropdown();

                    return; // Halt checkout row construction execution
                }
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
            updateTransactionTotal();
            barcodeField.clear();
        });
    }
    private void triggerSuggestionsDropdown() {
        // Force JavaFX to run this at the end of the current frame cycle
        Platform.runLater(() -> {
            // 1. Regain focus immediately to capture user typing
            barcodeField.requestFocus();

            // 2. Position the carot cursor cleanly at the end of the text string
            barcodeField.end();

            if (suggestionPopup != null) {
                // 3. Clear old visual state artifacts if any exist
                if (suggestionPopup.isShowing()) {
                    suggestionPopup.hide();
                }
            }
            // 4. Force display layout placement immediately beneath the text field
            suggestionPopup.show(barcodeField.getScene().getWindow());

            // 5. OPTIONAL: If your suggestion engine does not automatically refresh
            // when shown, manually fire a pseudo-text event to trigger the filtering logic:
            String currentText = barcodeField.getText();
            if (currentText != null && !currentText.isEmpty()) {
                // This forces your skin/popup selection listeners to run their filtering loops right now
                barcodeField.setText("");
                barcodeField.setText(currentText);
                barcodeField.end(); // Keep carot at the edge
            }
        });
    }
    // from now on, the rest will involve searching and backend related tasks, we stop here for now

    private void saveSale() {
        if (saleItems.isEmpty()) {
            return;
        }

        SaleHeader header = new SaleHeader();
        header.setSaleUser(UserSession.getInstance().getUserId());
        header.setSaleNote("sale");

        // Clean string parsing to extract total amount safely
        String cleanTotalString = totalLabel.getText().replace("DA", "").trim();
        BigDecimal saleTotal = new BigDecimal(cleanTotalString);
        header.setSaleTotal(saleTotal);

        if (clientCheckBox.isSelected()) {
            // We will configure the paid/debt values for clients when you're ready to implement client billing
            // e.g., header.setSaleClient(selectedClient.getClientId());
            // header.setPaidAmount(new BigDecimal(paidAmountField.getText()));
        } else {
            // Anonymous cash customer pays the full total upfront
            header.setPaidAmount(saleTotal);
        }

        ProductsCache cache = ProductsCache.getInstance();
        List<SaleDetailItem> soldItems = new ArrayList<>();

        // 🛡️ Tracks total cost of goods sold (COGS) to find true net profit
        BigDecimal totalCostPrice = BigDecimal.ZERO;

        for (CartItem item : saleItems) {
            Product cachedProduct = cache.get(item.getProductId());

            if (cachedProduct != null) {
                BigDecimal itemQuantity = item.getQuantity();
                BigDecimal unitCost = cachedProduct.getCostPrice();

                // Multiply cost by quantity: (Cost * Qty) and accumulate
                BigDecimal lineItemTotalCost = unitCost.multiply(itemQuantity);
                totalCostPrice = totalCostPrice.add(lineItemTotalCost);
            }

            // Map CartItem state to the database model
            soldItems.add(
                    new SaleDetailItem(
                            0, // Will be replaced by the generated Sale Header ID upon insert
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    )
            );
        }

        // Profit = Total Sale Amount - Total Cost of Goods Sold
        // CONVERT PROFIT TO PAID AMOUNT - TOTAL COST WHEN THERE IS DEBT
        BigDecimal netProfit = saleTotal.subtract(totalCostPrice);
        header.setSaleProfit(netProfit);

        //Set current authenticated user ID on the header
        header.setSaleUser(UserSession.getInstance().getUserId());
        System.out.println(header.toString());
        for(SaleDetailItem item : soldItems){
            System.out.println(item.toString());
        }
        //main ui first saves header
        int saleId = saleHeaderRepository.saveSaleHeader(header);
        if(saleId == -1){
            // TODO: fail here, must handle and not save
            return;
        }
        for(SaleDetailItem item : soldItems){
            item.setSaleId(saleId);
        }
        //TODO: now create two threads and save here and there, and we are done
        salesThreadPool.submit(() ->{
            try {
                System.out.println("[Thread A] Starting SQLite write for sale ID: " + saleId);
                saleDetailsRepository.saveSaleDetails(soldItems);
                System.out.println("[Thread A] SQLite write completed successfully.");
            } catch (Exception e) {
                System.err.println("[Thread A] CRITICAL ERROR: Failed to save details to database!");
                e.printStackTrace();
            }
        });
        salesThreadPool.submit(() -> {
            System.out.println("[Thread B] Starting file I/O write for sale ID: " + saleId);
            //TODO: create a class to handle logs and write everywhere, I have no idea if it needs to be interfaced or not

            AuditLogsService.getInstance().log(header, soldItems);
            System.out.println("[Thread B] File audit logging completed.");
        });
        saleItems.clear();       // Wipe the checkout table
        barcodeField.clear();
    }

    private void setClientNameFieldBehaviour() {
        // 1. Create a single ContextMenu instance out here so it persists across keystrokes
        ContextMenu suggestionPopup = new ContextMenu();
        ClientsCache cache = ClientsCache.getInstance();

        clientNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Hide the popup immediately if the field is cleared or text is too short
            if (newVal == null || newVal.trim().length() < 2) {
                suggestionPopup.hide();
                return;
            }

            String query = newVal.trim().toLowerCase();
            suggestionPopup.getItems().clear(); // Wipe the previous keystroke's results

            // 2. Sweep the memory cache structures for matches

            List<Client> matchedClients = new ArrayList<>();

            // Loop through the cache map values to find matching strings
            for (org.example.vendly_remastered.cache.model.ClientCacheLine line : cache.getCacheMapValues()) {
                Client client = line.getClient();
                if (client == null) continue;

                boolean matchesFullName = client.getContactFullName() != null &&
                        client.getContactFullName().toLowerCase().contains(query);
                boolean matchesCompany = client.getCompanyName() != null &&
                        client.getCompanyName().toLowerCase().contains(query);

                if (matchesFullName || matchesCompany) {
                    matchedClients.add(client);
                }
            }

            // 3. Hide if no records are resident in memory
            if (matchedClients.isEmpty()) {
                suggestionPopup.hide();
                return;
            }

            // 4. Transform matches into selectable UI MenuItems
            for (Client client : matchedClients) {
                String displayName = client.getContactFullName();
                if (client.getCompanyName() != null && !client.getCompanyName().isBlank()) {
                    displayName += " (" + client.getCompanyName() + ")";
                }

                MenuItem item = new MenuItem(displayName);

                // Handle item selection action
                item.setOnAction(e -> {
                    clientNameField.setText(client.getContactFullName());
                    suggestionPopup.hide();
                });

                suggestionPopup.getItems().add(item);
            }

            // 5. Anchor the suggestion popup right under the text input field box
            if (!suggestionPopup.isShowing()) {
                suggestionPopup.show(clientNameField, javafx.geometry.Side.BOTTOM, 0, 0);
            }
        });

        // Safeguard: Dismiss popup if the field loses operational focus
        clientNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestionPopup.hide();
            }
        });
        clientNameField.setOnAction(_ -> {
            Client c = cache.getByName(clientNameField.getText().trim().toLowerCase());
            if(c == null){
                // todo: client is not present
            } else if(c.getContactFullName().equals("rollbackOption")){
                clientNameField.setText(clientNameField.getText());
                //todo: trigger the suggestions again
            } else {
                //found write full name there
            }
        });
    }
}

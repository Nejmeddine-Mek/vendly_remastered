package org.example.vendly_remastered.controls;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.cache.ProductsCache;
import org.example.vendly_remastered.constants.Constants;
import org.example.vendly_remastered.models.dto.CartItem;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.models.entity.SaleDetailItem;
import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISaleDetailsRepository;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;
import org.example.vendly_remastered.repository.IStockRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.service.AuditLogsService;
import org.example.vendly_remastered.service.ErrorLogsService;
import org.example.vendly_remastered.service.SalesServices;

import org.example.vendly_remastered.utils.ControllerDispatcher;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SalesViewController implements Initializable {
    @FXML private TextField quantityField;
    @FXML private TextField priceField;
    @FXML private TextField barcodeField;
    @FXML private CheckBox clientCheckBox;
    @FXML private TextField clientNameField;
    @FXML private Label paidAmountLabel;
    @FXML private TextField paidAmountField;
    @FXML private Label clientNameLabel;
    @FXML private Label debtsLabel;

    // those are to fix later
    @FXML private TableView<CartItem> saleItemsTable;
    @FXML private TableColumn<CartItem, String> colBarcode;
    @FXML private TableColumn<CartItem, String> colProduct;
    @FXML private TableColumn<CartItem, BigDecimal> colQuantity;
    @FXML private TableColumn<CartItem, BigDecimal> colUnitPrice;
    @FXML private TableColumn<CartItem, BigDecimal> colSubTotal;

    @FXML private CheckBox lossCheckBox;
    @FXML private CheckBox returnCheckBox;

    @FXML private Button queueButton;
    @FXML private Label totalLabel;
    @FXML private Button confirmButton;
    @FXML private Button noBarCodeButton;


    private final String searchPrefix = ".";
    private final ContextMenu suggestionPopup = new ContextMenu();
    ObservableList<CartItem> saleItems;

    // A thread pool with 2 dedicated worker threads for background operations
    private final ExecutorService salesThreadPool = Executors.newFixedThreadPool(2);
    ISalesHeaderRepository saleHeaderRepository;
    ISaleDetailsRepository saleDetailsRepository;
    IStockRepository stockRepository;

    public void setStockRepository(IStockRepository stockRepository){ this.stockRepository = stockRepository; }

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
        // tracking clients' debts here
        clientCheckBox.visibleProperty().bind(Config.getInstance().trackClientsDebtsProperty());
        clientCheckBox.managedProperty().bind(clientCheckBox.visibleProperty());
        debtsLabel.setText("");
        paidAmountField.setText("0.0");
        setQuantityBehaviour();
        setPriceBehaviour(0.0);

        setDefaultClientFields();
        setClientCheckBoxBehaviour();
        setTextFormatting();
        setTableBehaviour();
        setColumnsBehaviour();
        setConfirmButtonBehaviour();
        setClientNameFieldBehaviour();

        // Helper method or inline logic to enforce mutual exclusion
        ChangeListener<Boolean> mutualExclusionListener = (obs, _, newVal) -> {
            if (newVal) {
                if (obs == returnCheckBox.selectedProperty()) {
                    lossCheckBox.setSelected(false);
                } else if (obs == lossCheckBox.selectedProperty()) {
                    returnCheckBox.setSelected(false);
                }
            }
        };

        returnCheckBox.selectedProperty().addListener(mutualExclusionListener);
        lossCheckBox.selectedProperty().addListener(mutualExclusionListener);
        queueButton.setOnAction(_ -> {
            try {
                // 1. Load a fresh instance of the sales window FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.getViewPath(Constants.SALES_VIEW)));

                Config config = Config.getInstance();
                loader.setResources(config.getLanguageBundle());

                Parent root = loader.load();

                config.applyOrientation(root);
                Object controller = loader.getController();
                if(controller != null)
                    ControllerDispatcher.execute(controller);

                // 2. Create and show a new Stage (Window)
                Stage queueStage = new Stage();
                queueStage.setTitle("Queued Sale - " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
                queueStage.setScene(new Scene(root));
                queueStage.show();
                Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/vendly_remastered_icon.png")));
                queueStage.getIcons().add(
                        appIcon
                );
                // 3. Reset the current active sale view so it starts fresh
                // resetCurrentSale(); // Uncomment when ready to clear current window

            } catch (IOException e) {
                String message = "Failed to load layout view component: Sale View [Queue]";
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, e);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        noBarCodeButton.setOnAction(_ -> {
            setShowUncodedProducts();
        });
        Platform.runLater(() -> barcodeField.requestFocus() );
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
            }
        });
        quantityField.setOnAction(_ -> {
            commitQuantityAndReset();
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
        if(selectedItem == null && !saleItems.isEmpty()){
            selectedItem = saleItems.getFirst();
        }

        if (selectedItem != null) {
            selectedItem.setQuantityForPromo(validatedQty);
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
        priceField.setText(String.format("%.2f", price));
        setBarcodeFieldBehaviour();
    }
    private void setClientCheckBoxBehaviour(){
        clientCheckBox.selectedProperty().addListener((_, _, newValue) ->{
            if(newValue){
                debtsLabel.setVisible(true);
                debtsLabel.setManaged(true);
                clientNameField.setVisible(true);
                clientNameField.setManaged(true);
                clientNameLabel.setVisible(true);
                paidAmountField.setVisible(true);
                paidAmountField.setManaged(true);
                paidAmountLabel.setVisible(true);
            } else {
                debtsLabel.setVisible(false);
                debtsLabel.setManaged(false);
                clientNameField.setVisible(false);
                clientNameField.setManaged(false);
                clientNameLabel.setVisible(false);
                paidAmountField.setVisible(false);
                paidAmountField.setManaged(false);
                paidAmountLabel.setVisible(false);
            }
        });
    }
    private void setDefaultClientFields(){
        clientNameField.setPromptText(Config.getInstance().getLanguageBundle().getString("common.clientNamePrompt"));
        clientNameField.setVisible(false);
        clientNameField.setManaged(false);
        clientNameLabel.setVisible(false);
        paidAmountField.setPromptText(Config.getInstance().getLanguageBundle().getString("common.paidAmountPrompt"));
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

        saleItemsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newItem) -> {
            if(newItem == null){
                String lastQuantity = "1.00";
                if(!saleItems.isEmpty()){
                    lastQuantity = String.format("%.2f", saleItems.getFirst().getQuantity());
                }
                priceField.setText("0.00");
                quantityField.setText(lastQuantity);
            } else {
                priceField.setText(String.format("%.2f", newItem.getUnitPrice()));
                quantityField.setText(String.format("%.2f", newItem.getQuantity()));
            }
        });

        saleItemsTable.setOnMouseClicked(click -> {
            if(click.getClickCount() == 2){
                CartItem selectedItem = saleItemsTable.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    saleItems.remove(selectedItem);
                    updateTransactionTotal();
                }
            }
        });
    }
    private void setColumnsBehaviour(){
        colBarcode.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(saleItemsTable.getItems().indexOf(cellData.getValue()) + 1)
        ));
        colBarcode.setResizable(true);
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colProduct.setResizable(true);
        colQuantity.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
        colQuantity.setResizable(true);
        colUnitPrice.setCellValueFactory(cellData -> cellData.getValue().unitPriceProperty());
        colUnitPrice.setResizable(true);
        colSubTotal.setCellValueFactory(cellData -> cellData.getValue().subtotalProperty());
        colSubTotal.setResizable(true);
    }

    private void setBarcodeFieldBehaviour() {
        // Ensure you have a single ContextMenu instance accessible in this scope
        // ContextMenu suggestionPopup = new ContextMenu(); // If not declared at class level
        ProductsCache cache = ProductsCache.getInstance();
        barcodeField.requestFocus();

        // 1. Switch focus to quantity field using ALT key
        barcodeField.setOnKeyPressed(key -> {
            if (barcodeField.isFocused() && key.getCode() == KeyCode.SPACE) {
                saveSale();
                Platform.runLater(key::consume);
            }
            if (barcodeField.isFocused() && (key.getCode() == KeyCode.PLUS || key.getCode() == KeyCode.ADD)) {
                increment();
                Platform.runLater(barcodeField::clear);
                Platform.runLater(key::consume);
            }
            if (barcodeField.isFocused() && (key.getCode() == KeyCode.MINUS || key.getCode() == KeyCode.SUBTRACT)) {
                decrement();
                Platform.runLater(barcodeField::clear);
                Platform.runLater(key::consume);
            }
            if (barcodeField.isFocused() && key.isAltDown()) {
                quantityField.setEditable(true);
                quantityField.requestFocus();
                Platform.runLater(key::consume);
            }
        });

        // 2. Text Listener to intercept typing for '@' search suggestions
        barcodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateAndShowProductSuggestions(newVal, suggestionPopup, cache);
        });

        // Safeguard: Dismiss popup if the field loses operational focus
        barcodeField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestionPopup.hide();
            }
        });

        // 3. Handle barcode input / Enter key action
        barcodeField.setOnAction(_ -> {
            String inputBarcode = barcodeField.getText();
            if (inputBarcode == null || inputBarcode.trim().isEmpty()) {
                quantityField.setEditable(true);
                quantityField.requestFocus();
                return;
            }

            String targetBarcode = inputBarcode.trim();
            Product targetProduct = null;

            // CASE A: Searching by name via '.' prefix
            if (targetBarcode.startsWith(searchPrefix)) {
                String query = targetBarcode.substring(1).trim().toLowerCase();
                System.out.println("Looking for product: " + query);

                // Expected backend change: searchByName now returns a List<Product> matching the query
                List<Product> matches = cache.searchByName(query);

                if (matches.isEmpty()) {
                    System.out.println("Product Name [" + query + "] was not found");
                    return;
                } else if(matches.size() > 1){
                    // Multiple options found! Retrigger the popup directly with current input
                    updateAndShowProductSuggestions(targetBarcode, suggestionPopup, cache);
                    barcodeField.end(); // Keep cursor text caret at the very end
                    return; // Halt cart entry execution
                } else {
                    targetProduct = matches.getFirst();
                }
            }
            // CASE B: Searching directly by standard barcode
            else {
                targetProduct = cache.getByBarcode(targetBarcode);
            }

            if (targetProduct == null) {
                barcodeField.selectAll();
                System.out.println("Product with barcode or Name " + targetBarcode + " was not found");
                return;
            }

            // 4. Check if the product is already inside your cart list
            final int targetId = targetProduct.getProductId();
            Optional<CartItem> existingItemOpt = saleItems.stream()
                    .filter(item -> item.getProductId() == targetId)
                    .findFirst();

            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                BigDecimal currentQty = existingItem.getQuantity();
                existingItem.setQuantity(currentQty.add(BigDecimal.ONE));
            } else {
                CartItem newItem = new CartItem(
                        targetProduct.getProductId(),
                        targetProduct.getBarcode(), // Use actual product's barcode instead of potentially text "@name"
                        targetProduct.getProductName(),
                        targetProduct.getRetailPrice(),
                        BigDecimal.ONE
                );
                if (targetProduct.getPromotionPrice() != null && targetProduct.getPromotionQuantity() != null) {
                    newItem.setPromotionPrice(targetProduct.getPromotionPrice());
                    newItem.setPromotionQuantity(targetProduct.getPromotionQuantity());
                }
                quantityField.setText(String.format("%.2f", newItem.getQuantity()));
                priceField.setText(String.format("%.2f",newItem.getUnitPrice()));
                // this helps becuase when a new product is added, we want to shift focus directly to it
                saleItemsTable.getSelectionModel().clearSelection();
                saleItems.addFirst(newItem);
            }

            updateTransactionTotal();
            barcodeField.clear();
        });
    }

    private void decrement() {
        CartItem selectedItem = saleItemsTable.getSelectionModel().getSelectedItem();
        if(selectedItem == null && !saleItems.isEmpty())
            selectedItem = saleItems.getFirst();

        if(selectedItem != null) {
            BigDecimal qty = selectedItem.getQuantity();
            qty = qty.subtract(BigDecimal.ONE);
            if(qty.compareTo(BigDecimal.ZERO) == 0)
                saleItems.remove(selectedItem);
            else
                selectedItem.setQuantity(qty);
            updateTransactionTotal();
            saleItemsTable.refresh();
        }
    }

    private void increment() {
        CartItem selectedItem = saleItemsTable.getSelectionModel().getSelectedItem();
        if(selectedItem == null && !saleItems.isEmpty())
            selectedItem = saleItems.getFirst();

        if(selectedItem != null) {
            selectedItem.setQuantity(selectedItem.getQuantity().add(BigDecimal.ONE));
            updateTransactionTotal();
            saleItemsTable.refresh();
        }
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

        if(clientCheckBox.isSelected()) {
            String clientName = clientNameField.getText();
            Client c = ClientsCache.getInstance().searchByName(clientName);
            if(c == null){
                // TODO: Handle impossible case where client text doesn't match anything
                String message = "[Checkout Error] Selected client not found in cache.";
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, new Exception("Selected client not found in cache (impossible state)"));
                return;
            }
            header.setSaleClient(c.getClientId());
            BigDecimal paid = new BigDecimal(paidAmountField.getText().trim());
            header.setPaidAmount(paid);
            // 2. Clearer debt calculation: Sale - Paid
            // If positive -> Client owes you money (New Debt)
            // If negative -> Client overpaid (Reduces existing Debt)
            BigDecimal debtDifference = header.getSaleTotal().subtract(header.getPaidAmount());
            if (debtDifference.compareTo(BigDecimal.ZERO) > 0) {
                // Case A: Standard partial payment -> increase client's debt balance
                c.setCurrentDebts(c.getCurrentDebts().add(debtDifference));
                ClientsCache.getInstance().put(c,ClientsCache.UPDATE_INDEX);
            } else if (debtDifference.compareTo(BigDecimal.ZERO) < 0) {
                // this line is redundant, but resets the sale payment to saleTotal, mitigation comes next
                // Case B: Overpayment -> Cap this specific invoice at its sale total
                header.setPaidAmount(saleTotal);

                // The overpayment amount is simply the absolute value of our difference
                BigDecimal overpayment = debtDifference.abs();

                // Deduct the full overpayment from the client's total debt balance upfront
                c.setCurrentDebts(c.getCurrentDebts().subtract(overpayment));

                // Distribute the overpayment down into older historical invoices
                SalesServices saleService = new SalesServices(saleHeaderRepository);
                saleService.resolveDebts(overpayment, c.getClientId());

                // Sync the cache with the updated client entity
                ClientsCache.getInstance().put(c, ClientsCache.UPDATE_INDEX);
            }
        } else {
            // Anonymous cash customer pays the full total upfront
            header.setPaidAmount(saleTotal);
        }

        ProductsCache cache = ProductsCache.getInstance();
        List<SaleDetailItem> soldItems = new ArrayList<>();

        // 🛡️ Tracks total cost of goods sold (COGS) to find true net profit
        BigDecimal totalCostPrice = BigDecimal.ZERO;
        Map<Integer, BigDecimal> newStock = new HashMap<>();
        for (CartItem item : saleItems) {
            Product cachedProduct = cache.get(item.getProductId());
            newStock.put(item.getProductId(), item.getQuantity());
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
        BigDecimal netProfit = header.getSaleTotal().subtract(totalCostPrice);
        header.setSaleProfit(netProfit);

        //Set current authenticated user ID on the header
        header.setSaleUser(UserSession.getInstance().getUserId());
        if(Config.getInstance().isTrackStock()){
            Map<Integer, BigDecimal> remainingStocks = cache.updateStockAndCheckAlerts(newStock);
            for (Map.Entry<Integer, BigDecimal> entry : remainingStocks.entrySet()) {
                int productId = entry.getKey();
                BigDecimal currentStock = entry.getValue();

                Product p = cache.get(productId);
                if(Config.getInstance().isLowStockPOSAlert())
                    if (p != null && p.getMinStockLevel() != null) {
                        // Correct check: Is remaining stock less than or equal to the minimum threshold?
                        if (currentStock.compareTo(p.getMinStockLevel()) <= 0) {
                            // TODO: show alert
                            String message = "STOCK ALERT - LOW STOCK for product ID " + p.getProductId();
                            System.err.println(message);
                            ErrorLogsService.getInstance().log(message, new Exception("Low stock threshold breached"));
                        }
                    }
            }
        }

        if(returnCheckBox.isSelected()){
            header.setPaidAmount(header.getPaidAmount().negate());
            header.setSaleProfit(header.getSaleProfit().negate());
            header.setSaleTotal(header.getSaleTotal().negate());
            header.setSaleNote("return");
            returnCheckBox.setSelected(false);
        }
        if(lossCheckBox.isSelected()){
            header.setPaidAmount(BigDecimal.ZERO);
            header.setSaleProfit(header.getSaleTotal().negate());
            header.setSaleTotal(header.getSaleTotal().negate());
            header.setSaleNote("expired");
            lossCheckBox.setSelected(false);
        }
        //main ui first saves header
        int saleId = saleHeaderRepository.saveSaleHeader(header);
        if(saleId == -1){
            // TODO: fail here, must handle and not save
            String message = "[Checkout Error] Invalid sale ID (-1) encountered. Aborting save operation.";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, new IllegalStateException("Invalid sale ID encountered"));
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
            }catch (Exception e) {
                String message = "[Thread A] CRITICAL ERROR: Failed to save details to database!";
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, e);
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
        clientNameField.clear();
        paidAmountField.setText("0.0");
        debtsLabel.setText("");
        clientCheckBox.setSelected(false);
        totalLabel.setText("0.00 DA");

    }

    private void updateAndShowProductSuggestions(String textVal, ContextMenu suggestionPopup, ProductsCache cache) {
        if (textVal == null || !textVal.startsWith(searchPrefix)) {
            suggestionPopup.hide();
            return;
        }

        String query = textVal.substring(1).trim().toLowerCase();

        // Safety check for text length after the @ symbol
        if (query.isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        // Fetch matching items from cache or repository index
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
                // Fill with structural name and fire selection
                barcodeField.setText("." + prod.getProductName());
                suggestionPopup.hide();
                barcodeField.fireEvent(new javafx.event.ActionEvent()); // Re-trigger ActionEvent to add to cart
            });
            suggestionPopup.getItems().add(item);
        }

        // Show or keep anchor right under the barcode field
        if (!suggestionPopup.isShowing()) {
            suggestionPopup.show(barcodeField, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void setClientNameFieldBehaviour() {
        ContextMenu suggestionPopup = new ContextMenu();
        ClientsCache cache = ClientsCache.getInstance();

        // 1. Listen for typing changes
        clientNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            updateAndShowSuggestions(newVal, suggestionPopup, cache);
        });

        // Safeguard: Dismiss popup if the field loses operational focus
        clientNameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestionPopup.hide();
            }
        });

        // 2. Handle Enter key press action
        clientNameField.setOnAction(_ -> {
            String currentText = clientNameField.getText().trim();
            List<Client> clientMatches = cache.getByName(currentText.toLowerCase());

            if (clientMatches.isEmpty()) {
                // todo: client is not present
            } else {
                // Retrigger the matching pipeline manually using the exact text present
                updateAndShowSuggestions(currentText, suggestionPopup, cache);
            }
        });
    }

    // 3. Isolated core suggestion logic that can be explicitly invoked anywhere
    private void updateAndShowSuggestions(String textVal, ContextMenu suggestionPopup, ClientsCache cache) {
        // Hide the popup immediately if the field is cleared or text is too short
        if (textVal == null || textVal.trim().isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        String query = textVal.trim().toLowerCase();
        suggestionPopup.getItems().clear(); // Wipe the previous results


        // Loop through the cache map values to find matching strings
        List<Client> matchedClients = cache.getByName(textVal);

        // Hide if no records are resident in memory
        if (matchedClients.isEmpty()) {
            suggestionPopup.hide();
            return;
        }

        // Transform matches into selectable UI MenuItems
        for (Client client : matchedClients) {
            String displayName = client.getContactFullName();
            if (client.getCompanyName() != null && !client.getCompanyName().isBlank()) {
                displayName += " (" + client.getCompanyName() + ")";
            }

            MenuItem item = new MenuItem(displayName);

            // Handle item selection action
            item.setOnAction(e -> {
                clientNameField.setText(client.getContactFullName());
                debtsLabel.setText(java.text.MessageFormat.format(Config.getInstance().getLanguageBundle().getString("common.currentDebts"), client.getCurrentDebts()));
                suggestionPopup.hide();
            });

            suggestionPopup.getItems().add(item);
        }

        // Anchor the suggestion popup right under the text input field box
        if (!suggestionPopup.isShowing()) {
            suggestionPopup.show(clientNameField, javafx.geometry.Side.BOTTOM, 0, 0);
        }
    }

    private void setShowUncodedProducts(){
        try {
            Config config = Config.getInstance();
            // 1. Point to your wide layout FXML file
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/org/example/vendly_remastered/NoBarCodeSelection.fxml")));
            loader.setResources(config.getLanguageBundle());
            Parent root = loader.load();

            config.applyOrientation(root);

            // 2. Get controller and inject dependencies
            NoBarCodeSelectionController controller = loader.getController();
            ControllerDispatcher.execute(controller);

            // 3. 🌟 THE HANDOFF: Pass the callback to handle the selected product
            controller.setOnProductSelected( selectedProduct -> {
                if(selectedProduct != null){
                    CartItem prod = new CartItem(
                            selectedProduct.getProductId(),
                            "",
                            selectedProduct.getProductName(),
                            selectedProduct.getRetailPrice(),
                            BigDecimal.ONE
                    );
                    saleItems.addFirst(prod);
                    priceField.setText(String.valueOf(prod.getUnitPrice()));
                    updateTransactionTotal();
                    saleItemsTable.refresh();
                    barcodeField.requestFocus();
                }
            });

            // 4. Build and present the window scene configuration
            Stage modalStage = new Stage();
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/vendly_remastered_icon.png")));
            modalStage.getIcons().add(appIcon);

            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(Config.getInstance().getLanguageBundle().getString("common.searchUncodedProducts"));
            modalStage.setScene(new Scene(root));


            // Execution pauses right here until the user double-clicks a product (which calls closeModal)
            modalStage.showAndWait();

            // Execution safely resumes here once the window closes

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
            alert.showAndWait();
            String message = "Failed to load layout view component: Uncoded Products View";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, ex);
        }
    }
}

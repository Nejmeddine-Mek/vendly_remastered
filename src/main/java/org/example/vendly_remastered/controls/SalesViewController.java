package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import org.example.vendly_remastered.models.dto.CartItem;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // language file first
        //set default behaviour of fields
        setQuantityBehaviour();
        setPriceBehaviour(0.0);
        setDefaultClientFields();
        setClientCheckBoxBehaviour();
        setTextFormatting();

    }


    // let us define the behaviour of each field in a separate private method
    private void setQuantityBehaviour(){
        // we must create a formatter for this to accept only numeric values
        quantityField.setText("1.00");
        quantityField.setEditable(false);
        // Basically this works as follows
        // read a product then press enter
        // focus on the quantity field, we add it after all defaults are ready
    }

    private void setPriceBehaviour(double price){
        priceField.setEditable(false);
        priceField.setText(String.valueOf(price));
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

    }
    private void setColumnsBehaviour(){
        colBarcode.setCellValueFactory(cellData -> cellData.getValue().barCodeProperty());
        colProduct.setCellValueFactory(cellData -> cellData.getValue().productNameProperty());
        colQuantity.setCellValueFactory(cellData -> cellData.getValue().quantityProperty());
        colUnitPrice.setCellValueFactory(cellData -> cellData.getValue().unitPriceProperty());
        colSubTotal.setCellValueFactory(cellData -> cellData.getValue().subtotalProperty());
    }
    // from now on, the rest will involve searching and backend related tasks, we stop here for now
}

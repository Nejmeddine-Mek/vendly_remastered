package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class ClearDebtsController implements Initializable {
    @FXML private Label clientNameLabel;
    @FXML private Label currentDebtLabel;
    @FXML private TextField paymentAmountField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    Client selectedClient;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        paymentAmountField.setTextFormatter(TextFormatterUtils.createDecimalFormatter());
        saveButton.setOnAction(_ -> {
            if(paymentAmountField.getText().isEmpty())
                return;
            savePayment(paymentAmountField.getText());
        });
        cancelButton.setOnAction(_ -> closeWindow());
    }
    private void savePayment(String fieldContent){
        BigDecimal paidValue =new BigDecimal(fieldContent);
        BigDecimal remaining = selectedClient.getCurrentDebts().subtract(paidValue);
        if(remaining.compareTo(BigDecimal.ZERO) < 0)
            remaining = BigDecimal.ZERO;
        selectedClient.setCurrentDebts(remaining);
        ClientsCache cache = ClientsCache.getInstance();
        cache.put(selectedClient,ClientsCache.UPDATE_INDEX);
        closeWindow();
    }
    private void closeWindow() {
        if (cancelButton != null && cancelButton.getScene() != null) {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    public void setMode(Client targetClient) {
        this.selectedClient = targetClient;

        if (targetClient != null) {
            clientNameLabel.setText(targetClient.getContactFullName());

            BigDecimal currentDebt = targetClient.getCurrentDebts() != null
                    ? targetClient.getCurrentDebts()
                    : BigDecimal.ZERO;

            currentDebtLabel.setText(String.format("%.2f DA", currentDebt));
        }
    }
}

package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.ISuppliersRepository;
import org.example.vendly_remastered.utils.TextFormatterUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ClearSupplierDebtsController implements Initializable {
    @FXML private VBox rootContainer;
    @FXML private Label supplierNameLabel;
    @FXML private Label currentDebtLabel;
    @FXML private TextField paymentAmountField;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    ISuppliersRepository suppliersRepository;
    Supplier selectedSupplier;
    private Consumer<Supplier> onPaymentSaved;

    public void setOnPaymentSaved(Consumer<Supplier> onPaymentSaved) {
        this.onPaymentSaved = onPaymentSaved;
    }

    public void setSuppliersRepository(ISuppliersRepository repository){ this.suppliersRepository = repository; }
    public void setMode(Supplier targetSupplier){
        this.selectedSupplier = targetSupplier;

        if (targetSupplier != null) {
            supplierNameLabel.setText(targetSupplier.getSupplierFullName());

            BigDecimal currentDebt = targetSupplier.getDebts() != null
                    ? targetSupplier.getDebts()
                    : BigDecimal.ZERO;

            currentDebtLabel.setText(String.format("%.2f DA", currentDebt));
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveButton.setOnAction(_ -> {
            paymentAmountField.setTextFormatter(TextFormatterUtils.createDecimalFormatter());
            if(paymentAmountField.getText().isEmpty())
                return;
            savePayment(paymentAmountField.getText());
        });
        cancelButton.setOnAction(_ -> closeWindow());
    }

    private void savePayment(String fieldContent){
        BigDecimal paidValue =new BigDecimal(fieldContent);
        BigDecimal remaining = selectedSupplier.getDebts().subtract(paidValue);
        if(remaining.compareTo(BigDecimal.ZERO) < 0)
            remaining = BigDecimal.ZERO;
        selectedSupplier.setDebts(remaining);
        suppliersRepository.updateSupplier(selectedSupplier);

        if (onPaymentSaved != null) {
            onPaymentSaved.accept(selectedSupplier);
        }

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
}

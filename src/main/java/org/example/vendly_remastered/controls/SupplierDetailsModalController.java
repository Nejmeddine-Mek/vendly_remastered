
package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
        import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.ISuppliersRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.security.UserSession;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class SupplierDetailsModalController implements Initializable {

    // Header & Actions
    @FXML private Label lblModalTitle;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    // Form Input Controls
    @FXML private TextField txtSupplierCompany;
    @FXML private TextField txtSupplierFullName;
    @FXML private TextField txtPhoneNumber;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDebts;
    @FXML private CheckBox chkIsActive;
    ISuppliersRepository suppliersRepository;

    public void setSuppliersRepository(ISuppliersRepository suppliersRepository){ this.suppliersRepository = suppliersRepository; }
    private Supplier selectedSupplier;

    private Consumer<Supplier> onSupplierSaved;

    public void setOnSupplierSaved(Consumer<Supplier> onSupplierSaved) {
        this.onSupplierSaved = onSupplierSaved;
    }

    public void setMode(Supplier supplier, boolean READ_ONLY) {
        if(READ_ONLY){
            txtSupplierCompany.setEditable(false);
            txtSupplierFullName.setEditable(false);
            txtPhoneNumber.setEditable(false);
            txtEmail.setEditable(false);
            txtDebts.setEditable(false);
            chkIsActive.setDisable(true);
            btnCancel.setVisible(false);
            btnSave.setVisible(false);
        }
        this.selectedSupplier = supplier;

        if (supplier == null) {
            // --- CREATE MODE ---
            if (lblModalTitle != null) {
                lblModalTitle.setText(Config.getInstance().getLanguageBundle().getString("common.registerNewSupplier"));
            }

            // Reset all fields to clean initial state
            txtSupplierCompany.clear();
            txtSupplierFullName.clear();
            txtPhoneNumber.clear();
            txtEmail.clear();
            txtDebts.setText("0.00");
            txtDebts.setEditable(true); // Allow setting initial debt on creation
            chkIsActive.setSelected(true); // Default new accounts to active

        } else {
            // --- EDIT MODE ---
            if (lblModalTitle != null) {
                lblModalTitle.setText(java.text.MessageFormat.format(
                        Config.getInstance().getLanguageBundle().getString("common.editSupplier"),
                        supplier.getSupplierCompany() != null ? supplier.getSupplierCompany() : ""
                ));
            }

            // Safely populate text controls (handling potential null values)
            txtSupplierCompany.setText(supplier.getSupplierCompany() != null ? supplier.getSupplierCompany() : "");
            txtSupplierFullName.setText(supplier.getSupplierFullName() != null ? supplier.getSupplierFullName() : "");
            txtPhoneNumber.setText(supplier.getPhoneNumber() != null ? supplier.getPhoneNumber() : "");
            txtEmail.setText(supplier.getEmail() != null ? supplier.getEmail() : "");

            BigDecimal debt = supplier.getDebts() != null ? supplier.getDebts() : BigDecimal.ZERO;
            txtDebts.setText(String.format("%.2f", debt));
            txtDebts.setEditable(false); // Owed balance is readonly during edit

            // Active status
            chkIsActive.setSelected(supplier.isActive());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnSave.setOnAction(_ -> handleSaveAction());

        if (btnClose != null) {
            btnClose.setOnAction(_ -> closeModal());
        }
        if (btnCancel != null) {
            btnCancel.setOnAction(_ -> closeModal());
        }

        // Numeric sanitizer for debt field (allows digits and a single decimal point)
        txtDebts.textProperty().addListener((_, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*(\\.\\d*)?")) {
                txtDebts.setText(oldValue);
            }
        });
    }

    @FXML
    private void handleSaveAction() {
        // 1. Sanitize raw text inputs
        String companyName = txtSupplierCompany.getText() != null ? txtSupplierCompany.getText().trim() : "";
        String contactName = txtSupplierFullName.getText() != null ? txtSupplierFullName.getText().trim() : "";
        String phone = txtPhoneNumber.getText() != null ? txtPhoneNumber.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String debtInput = txtDebts.getText() != null ? txtDebts.getText().trim() : "0.00";
        boolean isActive = chkIsActive.isSelected();

        // 2. Validate required fields
        if (companyName.isEmpty()) {
            showAlert(Config.getInstance().getLanguageBundle().getString("common.validationError"), Config.getInstance().getLanguageBundle().getString("common.companyNameRequired"));
            txtSupplierCompany.requestFocus();
            return;
        }

        if (contactName.isEmpty()) {
            showAlert(Config.getInstance().getLanguageBundle().getString("common.validationError"), Config.getInstance().getLanguageBundle().getString("common.contactPersonRequired"));
            txtSupplierFullName.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            showAlert(Config.getInstance().getLanguageBundle().getString("common.validationError"), Config.getInstance().getLanguageBundle().getString("common.phoneNumber"));
            txtPhoneNumber.requestFocus();
            return;
        }

        // 3. Format/Pattern validations
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Config.getInstance().getLanguageBundle().getString("common.validationError"), Config.getInstance().getLanguageBundle().getString("common.validEmailRequired"));
            txtEmail.requestFocus();
            return;
        }

        BigDecimal debts = BigDecimal.ZERO;
        if(Config.getInstance().isTrackSupplierDebts()){
            try {
                if (!debtInput.isEmpty()) {
                    debts = new BigDecimal(debtInput);
                }
            } catch (NumberFormatException e) {
                showAlert(Config.getInstance().getLanguageBundle().getString("common.validationError"), Config.getInstance().getLanguageBundle().getString("common.invalidNumericDebts"));
                txtDebts.requestFocus();
                return;
            }
        }


        // 4. Construct or update target Supplier model
        if (selectedSupplier == null) {
            // --- ADD / INSERT PATH ---
            Supplier newSupplier = new Supplier();
            newSupplier.setSupplierCompany(companyName);
            newSupplier.setSupplierFullName(contactName);
            newSupplier.setPhoneNumber(phone);
            newSupplier.setEmail(email);
            newSupplier.setDebts(debts);
            newSupplier.setActive(isActive);
            newSupplier.setCreatedBy(UserSession.getInstance().getUserId());
            newSupplier.setCreatedByUsername(UserSession.getInstance().getUsername());

            int supplierId = suppliersRepository.AddSupplier(newSupplier);

            newSupplier.setSupplierId((long)supplierId);
            if (onSupplierSaved != null) {
                onSupplierSaved.accept(newSupplier);
            }
        } else {
            // --- UPDATE PATH ---
            boolean infoChanged = !companyName.equals(selectedSupplier.getSupplierCompany())
                    || !contactName.equals(selectedSupplier.getSupplierFullName())
                    || !nullSafeEquals(phone, selectedSupplier.getPhoneNumber())
                    || !nullSafeEquals(email, selectedSupplier.getEmail())
                    || isActive != selectedSupplier.isActive();

            if (infoChanged) {
                selectedSupplier.setSupplierCompany(companyName);
                selectedSupplier.setSupplierFullName(contactName);
                selectedSupplier.setPhoneNumber(phone);
                selectedSupplier.setEmail(email);
                selectedSupplier.setActive(isActive);

                // TODO: Update existing supplier in DB & refresh cache
                // SuppliersCache.getInstance().put(selectedSupplier, SuppliersCache.UPDATE_INDEX);
                suppliersRepository.updateSupplier(selectedSupplier);

                // Notify parent view
                if (onSupplierSaved != null) {
                    onSupplierSaved.accept(selectedSupplier);
                }
            }
        }

        // 5. Close modal
        closeModal();
    }

    private void closeModal() {
        if (btnClose != null && btnClose.getScene() != null) {
            Stage stage = (Stage) btnClose.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private boolean nullSafeEquals(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }
}
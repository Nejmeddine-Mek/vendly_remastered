package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.models.entity.Client;

import java.net.URL;
import java.util.ResourceBundle;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class ClientDetailsModalController implements Initializable {
    // Header & Actions
    @FXML private Label lblModalTitle;
    @FXML private Button btnClose;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    // Form Input Controls
    @FXML private TextField txtCompanyName;
    @FXML private TextField txtContactFullName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextArea txtAddress;
    @FXML private CheckBox chkIsActive;

    private Client selectedClient;


    public void setMode(Client client) {
        this.selectedClient = client;

        if (client == null) {
            // --- CREATE MODE ---
            if (lblModalTitle != null) {
                lblModalTitle.setText("Register New Client");
            }

            // Reset all fields to clean initial state
            txtCompanyName.clear();
            txtContactFullName.clear();
            txtPhone.clear();
            txtEmail.clear();
            txtAddress.clear();
            chkIsActive.setSelected(true); // Default new accounts to active

        } else {
            // --- EDIT MODE ---
            if (lblModalTitle != null) {
                lblModalTitle.setText("Edit Client: " + (client.getCompanyName() != null ? client.getCompanyName() : ""));
            }

            // Safely populate text controls (handling potential null values)
            txtCompanyName.setText(client.getCompanyName() != null ? client.getCompanyName() : "");
            txtContactFullName.setText(client.getContactFullName() != null ? client.getContactFullName() : "");
            txtPhone.setText(client.getPhone() != null ? client.getPhone() : "");
            txtEmail.setText(client.getEmail() != null ? client.getEmail() : "");
            txtAddress.setText(client.getAddress() != null ? client.getAddress() : "");

            // Active status
            chkIsActive.setSelected(client.isActive());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnSave.setOnAction(_ -> handleSaveAction());
    }
    @FXML
    private void handleSaveAction() {
        // 1. Sanitize raw text inputs
        String companyName = txtCompanyName.getText() != null ? txtCompanyName.getText().trim() : "";
        String contactName = txtContactFullName.getText() != null ? txtContactFullName.getText().trim() : "";
        String phone = txtPhone.getText() != null ? txtPhone.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String address = txtAddress.getText() != null ? txtAddress.getText().trim() : "";
        boolean isActive = chkIsActive.isSelected();

        // 2. Validate required fields
        if (companyName.isEmpty()) {
            showAlert("Validation Error", "Company Name is required.");
            txtCompanyName.requestFocus();
            return;
        }

        if (contactName.isEmpty()) {
            showAlert("Validation Error", "Contact Person Name is required.");
            txtContactFullName.requestFocus();
            return;
        }

        // 3. Format/Pattern validations (Optional basic checks)
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert("Validation Error", "Please enter a valid email address.");
            txtEmail.requestFocus();
            return;
        }
// 4. Construct or update target Client model and handle write-through cache operations
        if (selectedClient == null) {
            // --- ADD / INSERT PATH ---
            Client newClient = new Client();
            newClient.setCompanyName(companyName);
            newClient.setContactFullName(contactName);
            newClient.setPhone(phone);
            newClient.setEmail(email);
            newClient.setAddress(address);
            newClient.setActive(isActive);

            // Write-Through: Inserts new client into SQLite & updates in-memory cache
            ClientsCache.getInstance().put(newClient, ClientsCache.UPDATE_INDEX);

        } else {
            // --- UPDATE PATH ---


            // Check if profile/contact info changed (INFO_BIT slice)
            boolean infoChanged = !companyName.equals(selectedClient.getCompanyName())
                    || !contactName.equals(selectedClient.getContactFullName())
                    || !nullSafeEquals(phone, selectedClient.getPhone())
                    || !nullSafeEquals(email, selectedClient.getEmail())
                    || !nullSafeEquals(address, selectedClient.getAddress())
                    || isActive != selectedClient.isActive();

            if (infoChanged) {
                selectedClient.setCompanyName(companyName);
                selectedClient.setContactFullName(contactName);
                selectedClient.setPhone(phone);
                selectedClient.setEmail(email);
                selectedClient.setAddress(address);
                selectedClient.setActive(isActive);

                ClientsCache.getInstance().put(selectedClient, ClientsCache.UPDATE_INDEX);
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

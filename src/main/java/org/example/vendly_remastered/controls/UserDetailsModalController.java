package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Role;
import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IRoleRepository;


import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class UserDetailsModalController implements Initializable {
    @FXML private Label userDetailsLabel;
    @FXML private TextField txtUserId;
    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private ComboBox<String> comboRole;
    @FXML private TextField txtPassword;
    @FXML private TextField txtConfirmPassword;
    @FXML private Button userModifyButton;
    @FXML private Button toggleStatusButton;

    private User selectedUser;
    private boolean saveClicked;
    private Map<String, Short> roleNameToIdMap = new HashMap<>();
    private IRoleRepository roleRepository;

    public void setRoleRepository(IRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        handleSaveAction();

    }
    public void setComboRoleValues(){
        List<Role> roles = roleRepository.findAll();

        // Clear the map and combo box to prevent duplicates if recalled
        roleNameToIdMap.clear();
        this.comboRole.getItems().clear();
        for (Role role : roles) {
            // 🔑 Map the visible Name to its hidden Database ID
            roleNameToIdMap.put(role.getRoleName(), role.getRoleId());
            this.comboRole.getItems().add(role.getRoleName());
        }
        if (selectedUser == null || selectedUser.getRoleName() == null) {
            this.comboRole.setValue("Choose a role for the user");
        } else {
            this.comboRole.setValue(selectedUser.getRoleName());
        }
    }

    private void handleSaveAction() {
        if (!isValidInput()) return;

        // If it's a new user, instantiate a fresh entity container
        if (selectedUser == null) {
            selectedUser = new User();
            selectedUser.setIsActive(true); // Default status for new profiles
        }

        // Bind current form field parameters directly to the entity state
        selectedUser.setUsername(txtUsername.getText().trim());
        selectedUser.setFullName(txtFullName.getText().trim());
        selectedUser.setRoleName(comboRole.getValue());

        // Only update password fields if the operator typed a new value
        if (txtPassword.getText() != null && !txtPassword.getText().isEmpty()) {
            selectedUser.setPassword(txtPassword.getText()); // Hash this before passing if needed!
        }

        this.saveClicked = true;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) userModifyButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    private boolean isValidInput() {
        // 1. Core fields validation
        if (txtUsername.getText() == null || txtUsername.getText().trim().isEmpty() ||
                txtFullName.getText() == null || txtFullName.getText().trim().isEmpty() ||
                comboRole.getValue() == null) {
            return false;
        }

        // 2. Strict password matching logic (Mandatory for new user accounts)
        String pwd = txtPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();

        if (selectedUser == null) {
            // Creation mode requires a valid password match right away
            if (pwd == null || pwd.trim().isEmpty() || !pwd.equals(confirmPwd)) {
                return false;
            }
        } else {
            // Edit mode: Only validate if they are explicitly trying to update it
            if ((pwd != null && !pwd.isEmpty()) || (confirmPwd != null && !confirmPwd.isEmpty())) {
                if (pwd == null || !pwd.equals(confirmPwd)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isSaveClicked() {
        return this.saveClicked;
    }

    public User getUserChanges() {
        return this.selectedUser;
    }

    public void setMode(User user) {
        this.selectedUser = user;
        this.saveClicked = false;

        if (user == null) {
            // 🌟 CREATION MODE SETUP
            userDetailsLabel.setText("CREATE NEW ACCOUNT");
            userModifyButton.setText("Register New User");
            toggleStatusButton.setText("Cancel");
            txtUserId.setText("Auto-Generated");
            txtUserId.setDisable(true);
        } else {
            // 🔒 EDIT MODE SETUP
            userDetailsLabel.setText("USER ACCOUNT PROFILE");
            userModifyButton.setText("Save Registry Changes");
            toggleStatusButton.setText(user.isIsActive() ? "Deactivate Account" : "Activate Account");

            // Populate form values from entity data
            txtUserId.setText(String.valueOf(user.getUserId()));
            txtUsername.setText(user.getUsername());
            txtFullName.setText(user.getFullName());
            comboRole.getSelectionModel().select(user.getRoleName());

            // Keep passwords blank on display for visual protection
            txtPassword.clear();
            txtConfirmPassword.clear();
        }
    }
}

package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class UsersViewController implements Initializable {
    @FXML private Label userDetailsLabel;
    @FXML private TextArea userDetailsArea;
    @FXML private Label userSearchLabel;
    @FXML private TextField searchField;

    @FXML private TableView<String> userTableView;
    @FXML private TableColumn<String, String> colUserId;
    @FXML private TableColumn<String, String> colUsername;
    @FXML private TableColumn<String, String> colFullName;
    @FXML private TableColumn<String, String> colRole;
    @FXML private TableColumn<String, String> colStatus;
    @FXML private TableColumn<String, String> colCreatedAt;
    @FXML private TableColumn<String, String> colLastLogin;

    @FXML private Button addUserButton;
    @FXML private Button userModifyButton;
    @FXML private Button toggleStatusButton;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // set the language bundle
        // set buttons behaviour
        // load users data
    }
}

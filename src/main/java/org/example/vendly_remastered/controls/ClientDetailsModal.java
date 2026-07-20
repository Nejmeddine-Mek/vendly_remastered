package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientDetailsModal implements Initializable {
    @FXML private Label lblModalTitle;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtDebtBalance;
    @FXML private TextField txtCreditLimit;
    @FXML private TextArea txtAddress;

    public void setMode(Client client) {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }
}

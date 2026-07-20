package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Client;

import org.example.vendly_remastered.repository.IClientsRepository;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.net.URL;
import java.util.ResourceBundle;

public class ClientsViewController implements Initializable {
    @FXML private Label clientDetailsLabel;
    @FXML private TextArea clientDetailsArea;
    @FXML private Label clientSearchLabel;
    @FXML private TextField searchField;
    @FXML private Button addClientButton;
    @FXML private Button clientDetailsButton;
    @FXML private Button processPaymentButton;

    @FXML private TableView<?> clientTableView; // Replace ? with your Client Object Model

    @FXML private TableColumn<?, Integer> colId;
    @FXML private TableColumn<?, String> colFirstName;
    @FXML private TableColumn<?, String> colLastName;
    @FXML private TableColumn<?, String> colPhone;
    @FXML private TableColumn<?, String> colEmail;
    @FXML private TableColumn<?, Integer> colPurchases;
    @FXML private TableColumn<?, Double> colDebts;

    private IClientsRepository clientsRepository;

    public void setClientsRepository(IClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
        addClientButton.setOnAction(_ -> openClientModal(null));
    }

    private void setTableColumnsBehaviour(){
        colId.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("contactFullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colPurchases.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        colDebts.setCellValueFactory(new PropertyValueFactory<>("currentDebts"));
    }

    public void initializeTable(){

    }


    private void openClientModal(Client client) {
        try {
            System.out.println("Opening modal");
            // 1. Point to your wide layout FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/ClientDetailsModal.fxml"));
            Parent root = loader.load();

            // 2. Dispatch dependencies (injects category repos, services, etc., via your framework dispatcher)
            Object controller = loader.getController();
            ControllerDispatcher.execute(controller);

            ClientDetailsModal modalController = loader.getController();

            // 4. Check if we are editing an existing product or adding a completely fresh item
            boolean isEditMode = (client != null);
            if (isEditMode) {
                modalController.setMode(client);
            }

            // 5. Build and present the window scene configuration
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(isEditMode ? "Edit Client Specifications" : "Register New Client Asset");
            modalStage.setScene(new Scene(root));

            modalStage.showAndWait();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
            alert.showAndWait();
            ex.printStackTrace();
        }
    }

}

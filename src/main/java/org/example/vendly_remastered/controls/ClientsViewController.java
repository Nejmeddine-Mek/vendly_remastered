package org.example.vendly_remastered.controls;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.repository.IClientsRepository;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.service.ErrorLogsService;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.example.vendly_remastered.service.MessageBoxService.showAlert;

public class ClientsViewController implements Initializable {
    @FXML private Label clientDetailsLabel;
    @FXML private TextArea clientDetailsArea;
    @FXML private Label clientSearchLabel;
    @FXML private TextField searchField;
    @FXML private Button addClientButton;
    @FXML private Button clientDetailsButton;
    @FXML private Button processPaymentButton;

    @FXML private Button refreshButton;

    @FXML private TableView<Client> clientTableView;

    @FXML private TableColumn<Client, Integer> colId;
    @FXML private TableColumn<Client, String> colFirstName;
    @FXML private TableColumn<Client, String> colLastName;
    @FXML private TableColumn<Client, String> colPhone;
    @FXML private TableColumn<Client, String> colEmail;
    @FXML private TableColumn<Client, Integer> colPurchases;
    @FXML private TableColumn<Client, Double> colDebts;

    private boolean READ_ONLY = false;
    private boolean isOpenModal = false;
    private IClientsRepository clientsRepository;
    private ObservableList<Client> clientMasterList = FXCollections.observableArrayList();

    public void setClientsRepository(IClientsRepository clientsRepository) {
        this.clientsRepository = clientsRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        refreshButton.setOnAction( _ ->{
            initializeTable();
        });
        ObservableBooleanValue showPaymentButton = Bindings.and(
                (ObservableBooleanValue) Config.getInstance().trackClientsDebtsProperty(),
                processPaymentButton.visibleProperty()
        );

        addClientButton.managedProperty().bind(addClientButton.visibleProperty());
        processPaymentButton.managedProperty().bind(showPaymentButton);
        if(!Config.getInstance().isTrackClientsDebts()){
            processPaymentButton.setVisible(false);
        }
        READ_ONLY = !UserSession.getInstance().hasPrivilege("can_edit_clients");
        if(READ_ONLY){
            addClientButton.setVisible(false);
            processPaymentButton.setVisible(false);
        }
        setTableColumnsBehaviour();

        // 1. New Client Action
        addClientButton.setOnAction(_ -> openClientModal(null));

        // 2. Edit Selected Client Action
        clientDetailsButton.setOnAction(_ -> {
            Client selected = clientTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openClientModal(selected);
            } else {
                showAlert("Selection Required", "Please select a client from the table to edit.");
            }
        });

        // 3. Settle Debt / Process Payment Placeholder Action
        setClearDebtButtonBehaviour();

        // 4. Initial load of the client table
        initializeTable();
    }

    private void setTableColumnsBehaviour() {
        colId.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colFirstName.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        colLastName.setCellValueFactory(new PropertyValueFactory<>("contactFullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        //colPurchases.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        colDebts.setCellValueFactory(new PropertyValueFactory<>("currentDebts"));
    }

    public void initializeTable() {
        clientTableView.setOnMouseClicked(click -> {
            if(click.getClickCount() == 2){
                if(isOpenModal)
                    return;
                Client selectedClient = clientTableView.getSelectionModel().getSelectedItem();
                if(selectedClient == null)
                    return;
                isOpenModal = true;
                READ_ONLY = true;
                openClientModal(selectedClient);
                isOpenModal = false;
                READ_ONLY = false;

                clientTableView.getSelectionModel().clearSelection();
            }
        });
        if (clientsRepository == null){
            System.err.println("[Client repository] NULL reference");
            return;
        }

        // Fetch clients from in-memory cache / repo
        List<Client> clients = clientsRepository.findAll();
        clientMasterList.setAll(clients);

        // Setup dynamic filtering via searchField
        FilteredList<Client> filteredData = new FilteredList<>(clientMasterList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(client -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String searchLower = newValue.toLowerCase().trim();

                if (client.getCompanyName() != null && client.getCompanyName().toLowerCase().contains(searchLower)) {
                    return true;
                }
                if (client.getContactFullName() != null && client.getContactFullName().toLowerCase().contains(searchLower)) {
                    return true;
                }
                if (client.getPhone() != null && client.getPhone().contains(searchLower)) {
                    return true;
                }
                return client.getEmail() != null && client.getEmail().toLowerCase().contains(searchLower);
            });
        });

        SortedList<Client> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(clientTableView.comparatorProperty());

        clientTableView.setItems(sortedData);
    }

    private void openClientModal(Client client) {
        try {
            Config config = Config.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/ClientDetailsModal.fxml"));
            loader.setResources(config.getLanguageBundle());
            Parent root = loader.load();
            config.applyOrientation(root);
            Object controller = loader.getController();
            ControllerDispatcher.execute(controller);

            ClientDetailsModalController modalController = loader.getController();

            boolean isEditMode = (client != null);
            modalController.setMode(client, READ_ONLY);

            Stage modalStage = new Stage();

            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/vendly_remastered_icon.png")));
            if(appIcon != null)
                modalStage.getIcons().add(
                        appIcon
                );
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(isEditMode ? "Edit Client Profile" : "Register New Client");
            modalStage.setScene(new Scene(root));

            // Wait until user closes modal
            modalStage.showAndWait();

            // Refresh table from local cache immediately after closing
            initializeTable();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load client modal panel: " + ex.getMessage());
            alert.showAndWait();

            String message = "Failed to load layout view component: Client Modal View";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, ex);

        }
    }
    private void setClearDebtButtonBehaviour() {
        processPaymentButton.setOnAction(_ -> {
            Client targetClient = clientTableView.getSelectionModel().getSelectedItem();
            if(targetClient == null){
                //TODO: handle
                return;
            }
            try {
                Config config = Config.getInstance();
                // 1. Point to your wide layout FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/ClearDebtsModal.fxml"));
                loader.setResources(config.getLanguageBundle());
                Parent root = loader.load();
                config.applyOrientation(root);
                // 2. Dispatch dependencies (injects category repos, services, etc., via your framework dispatcher)
                Object controller = loader.getController();
                ControllerDispatcher.execute(controller);

                ClearDebtsController modalController = loader.getController();

                // 4. Check if we are editing an existing product or adding a completely fresh item
                modalController.setMode(targetClient);

                // 5. Build and present the window scene configuration
                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.setTitle("Edit Client debts");
                modalStage.setScene(new Scene(root));

                // Execution pauses right here until the user clicks Save or Cancel
                modalStage.showAndWait();
                clientTableView.refresh();

            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
                alert.showAndWait();
                String message = "Failed to load layout view component: Clear Debts";
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, ex);
            }
        });
    }
}
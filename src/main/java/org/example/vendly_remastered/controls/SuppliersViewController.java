package org.example.vendly_remastered.controls;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
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
import org.example.vendly_remastered.models.entity.Supplier;
import org.example.vendly_remastered.repository.ISuppliersRepository;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.service.ErrorLogsService;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class SuppliersViewController implements Initializable {
    @FXML private TextField searchField;
    @FXML private Button addSupplierButton;
    @FXML private Button supplierDetailsButton;
    @FXML private Button processOrderButton;
    @FXML private Button refreshButton;
    @FXML private TableView<Supplier> supplierTableView; // Replace ? with your Supplier model data entity

    @FXML private TableColumn<Supplier, Integer> colId;
    @FXML private TableColumn<Supplier, String> colCompanyName;
    @FXML private TableColumn<Supplier, String> colContactName;
    @FXML private TableColumn<Supplier, String> colPhone;
    @FXML private TableColumn<Supplier, String> colEmail;
    @FXML private TableColumn<Supplier, String> colCreatedBy;
    @FXML private TableColumn<Supplier, BigDecimal> colBalanceOwed;

    ISuppliersRepository suppliersRepository;

    private boolean READ_ONLY = false;
    private boolean isOpenModal = false;
    private final ObservableList<Supplier> supplierList = FXCollections.observableArrayList();
    public void setSuppliersRepository(ISuppliersRepository suppliersRepository){ this.suppliersRepository = suppliersRepository; }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshButton.setOnAction(_ -> {
            initializeTable();
        });
        BooleanBinding showProcessOrder = Bindings.and(
                // Add any other visibility conditions here, or just your config flag:
                (ObservableBooleanValue) Config.getInstance().trackSupplierDebtsProperty(),
                // Optional: add any extra conditions like cart.notEmptyProperty(), etc.
                (ObservableBooleanValue) Config.getInstance().trackSupplierDebtsProperty()
        );
        processOrderButton.managedProperty().bind(showProcessOrder);
        addSupplierButton.managedProperty().bind(addSupplierButton.visibleProperty());
        READ_ONLY = !UserSession.getInstance().hasPrivilege("can_edit_suppliers");
        if(READ_ONLY){
            processOrderButton.setVisible(false);
            addSupplierButton.setVisible(false);

        }
        setClearDebtButtonBehaviour();
        setTableColumnsBehaviour();
        supplierDetailsButton.setOnAction(_ -> {
            setSupplierDetailsButtonBehaviour();
        });
        addSupplierButton.setOnAction(_ -> openModal(null));
    }
    private void setTableColumnsBehaviour() {
        colId.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        colCompanyName.setCellValueFactory(new PropertyValueFactory<>("supplierCompany"));
        colContactName.setCellValueFactory(new PropertyValueFactory<>("supplierFullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Displays createdBy integer directly (or username if Supplier model holds it)
        colCreatedBy.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getCreatedBy() != null ? cell.getValue().getCreatedBy().toString() : "—"
                )
        );

        // Formats BigDecimal debts cleanly as currency (e.g. 150000.00 DA)
        colBalanceOwed.setCellValueFactory(new PropertyValueFactory<>("debts"));
        colBalanceOwed.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f DA", balance));
                }
            }
        });
    }

    public void initializeTable() {
        supplierTableView.setOnMouseClicked(click -> {
            if(click.getClickCount() == 2){
                if(isOpenModal)
                    return;
                Supplier selectedSupplier = supplierTableView.getSelectionModel().getSelectedItem();
                if(selectedSupplier == null)
                    return;

                READ_ONLY = true;
                isOpenModal = true;
                openModal(selectedSupplier);

                READ_ONLY = false;
                isOpenModal = false;

                supplierTableView.getSelectionModel().clearSelection();
            }
        });

        if (suppliersRepository == null) {
            System.out.println("repo is null");
            return;
        }

        // Fetch suppliers from in-memory cache / repo
        List<Supplier> suppliers = suppliersRepository.findAll();
        supplierList.setAll(suppliers);

        // Setup dynamic filtering via searchField
        FilteredList<Supplier> filteredData = new FilteredList<>(supplierList, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(supplier -> {
                if (newValue == null || newValue.isBlank()) {
                    return true;
                }

                String searchLower = newValue.toLowerCase().trim();

                if (supplier.getSupplierCompany() != null && supplier.getSupplierCompany().toLowerCase().contains(searchLower)) {
                    return true;
                }
                if (supplier.getSupplierFullName() != null && supplier.getSupplierFullName().toLowerCase().contains(searchLower)) {
                    return true;
                }
                if (supplier.getPhoneNumber() != null && supplier.getPhoneNumber().contains(searchLower)) {
                    return true;
                }
                return supplier.getEmail() != null && supplier.getEmail().toLowerCase().contains(searchLower);
            });
        });

        SortedList<Supplier> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(supplierTableView.comparatorProperty());

        supplierTableView.setItems(sortedData);
    }
    private void setSearchBarBehaviour(){

    }
    private void setSupplierDetailsButtonBehaviour(){
        Supplier selectedSuuplier = supplierTableView.getSelectionModel().getSelectedItem();
        if(selectedSuuplier == null){
            // TODO: handle with an alert or so
            return;
        }
        openModal(selectedSuuplier);
    }
    private void openModal(Supplier supplier){
        try {
            Config config = Config.getInstance();
            // 1. Point to your wide layout FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/SupplierDetailsModal.fxml"));
            loader.setResources(config.getLanguageBundle());
            Parent root = loader.load();
            config.applyOrientation(root);
            // 2. Dispatch dependencies (injects category repos, services, etc., via your framework dispatcher)
            Object controller = loader.getController();
            ControllerDispatcher.execute(controller);

            SupplierDetailsModalController modalController = loader.getController();

            // 4. Check if we are editing an existing product or adding a completely fresh item
            modalController.setMode(supplier, READ_ONLY);

            modalController.setOnSupplierSaved(savedSupplier -> {
                if (supplier == null) {
                    // ADD: Insert the newly created supplier into master list
                    supplierList.add(savedSupplier);
                } else {
                    // EDIT: Force TableView to visually refresh changed properties
                    supplierTableView.refresh();
                }
            });
            // 5. Build and present the window scene configuration
            Stage modalStage = new Stage();
            Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/vendly_remastered_icon.png")));
            if(appIcon != null)
                modalStage.getIcons().add(
                        appIcon
                );
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Edit Client debts");
            modalStage.setScene(new Scene(root));

            // Execution pauses right here until the user clicks Save or Cancel
            modalStage.showAndWait();


        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
            alert.showAndWait();
            String message = "Failed to load layout view component: Supplier Details View";
            System.err.println(message);
            ErrorLogsService.getInstance().log(message, ex);
        }
    }

    private void setClearDebtButtonBehaviour() {
        processOrderButton.setOnAction(_ -> {
            Supplier targetSupplier = supplierTableView.getSelectionModel().getSelectedItem();
            if(targetSupplier == null){
                //TODO: handle
                return;
            }
            try {
                Config config = Config.getInstance();
                // 1. Point to your wide layout FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/ClearSupplierDebtsModal.fxml"));
                loader.setResources(config.getLanguageBundle());

                Parent root = loader.load();
                config.applyOrientation(root);

                // 2. Dispatch dependencies (injects category repos, services, etc., via your framework dispatcher)
                Object controller = loader.getController();
                ControllerDispatcher.execute(controller);

                ClearSupplierDebtsController modalController = loader.getController();


                // 4. Check if we are editing an existing product or adding a completely fresh item
                modalController.setMode(targetSupplier);

                modalController.setOnPaymentSaved(updatedSupplier -> {
                    // Forces the TableView to repaint cell values immediately
                    supplierTableView.refresh();
                });
                // 5. Build and present the window scene configuration
                Stage modalStage = new Stage();
                modalStage.initModality(Modality.APPLICATION_MODAL);
                modalStage.setTitle("Edit Client debts");
                modalStage.setScene(new Scene(root));

                // Execution pauses right here until the user clicks Save or Cancel
                modalStage.showAndWait();


            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load product modal UI panel: " + ex.getMessage());
                alert.showAndWait();
                ex.printStackTrace();
            }
        });
    }
}

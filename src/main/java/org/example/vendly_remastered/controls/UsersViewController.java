package org.example.vendly_remastered.controls;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.net.URL;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class UsersViewController implements Initializable {
    @FXML private Label userDetailsLabel;
    @FXML private TextArea userDetailsArea;
    @FXML private Label userSearchLabel;
    @FXML private TextField searchField;

    @FXML private TableView<User> userTableView;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colCreatedBy;
    @FXML private TableColumn<User, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<User, LocalDateTime> colLastLogin;

    @FXML private Button addUserButton;
    @FXML private Button userModifyButton;
    @FXML private Button toggleStatusButton;

    private IUserRepository userRepository;
    private FilteredList<User> filteredUsers;

    private FormMode currentMode = FormMode.VIEW_EDIT;

    public void setUserRepository(IUserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // set the language bundle
        // set buttons behaviour
        setTableColumnsBehaviour();
        setAddUserButtonBehaviour();
        setUserTableViewBehaviour();
        setSearchBarBehaviour();
        setUserModifyButtonBehaviour();
        setToggleStatusButtonBehaviour();

        // load users data
    }

    public void initializeTable(){
        if (userRepository == null) return;
        // Make sure to instantiate the observable list wrapper
        ObservableList<User> usersList = javafx.collections.FXCollections.observableArrayList();
        List<User> ul = userRepository.findAll();
        usersList.addAll(ul);
        this.filteredUsers = new FilteredList<>(usersList);
        userTableView.setItems(this.filteredUsers);
        userTableView.refresh();
    }

    private void setTableColumnsBehaviour(){
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<>("createdByUsername"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
    }

    private void setAddUserButtonBehaviour(){
        this.addUserButton.setOnAction(_ -> {
            setCurrentMode(FormMode.CREATION);
            openUserModal(null);
        });
    }

    private void setUserTableViewBehaviour(){
        userTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Populate the multi-line details area for viewing
                StringBuilder details = new StringBuilder();
                details.append("User ID: ").append(newSelection.getUserId()).append("\n")
                        .append("Username: ").append(newSelection.getUsername()).append("\n")
                        .append("Full Name: ").append(newSelection.getFullName()).append("\n")
                        .append("Role: ").append(newSelection.getRoleName()).append("\n")
                        .append("Status: ").append(newSelection.isIsActive() ? "Active" : "Inactive").append("\n")
                        .append("Created At: ").append(newSelection.getCreatedAt()).append("\n")
                        .append("Last Login: ").append(newSelection.getLastLogin() != null ? newSelection.getLastLogin() : "Never");

                userDetailsArea.setText(details.toString());
                userDetailsArea.setEditable(false); // Keep it read-only by default

                // Dynamically change the text of the toggle button based on selected user status
                toggleStatusButton.setText(newSelection.isIsActive() ? "Deactivate Account" : "Activate Account");
            }
        });
    }

    private void setUserModifyButtonBehaviour(){
        this.userModifyButton.setOnAction(_ ->{
            User selectedUser = this.userTableView.getSelectionModel().getSelectedItem();
            if(selectedUser != null){
                System.out.println(selectedUser.getFullName());
                this.setCurrentMode(FormMode.VIEW_EDIT);
                openUserModal(selectedUser);
            }
        });

    }

    private void setToggleStatusButtonBehaviour(){
        toggleStatusButton.setOnAction(event -> {
            User selectedUser = userTableView.getSelectionModel().getSelectedItem();
            if (selectedUser == null || userRepository == null) return;

            // Flip the current status state directly on the entity
            boolean newStatus = !selectedUser.isIsActive();
            selectedUser.setIsActive(newStatus);

            // Push changes straight to storage
            if (userRepository.save(selectedUser)) {
                // Refresh table items instantly
                userTableView.refresh();
                // Update toggle button string text contextually
                toggleStatusButton.setText(newStatus ? "Deactivate Account" : "Activate Account");
            }
        });
    }

    private void setSearchBarBehaviour() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredUsers == null) return;

            filteredUsers.setPredicate(user -> {
                // If search box is empty, show everything
                if (newValue == null || newValue.trim().isEmpty()) {
                    return true;
                }

                String query = newValue.toLowerCase().trim();

                // 🎯 CONCATENATION STRATEGY: Combine all search target parameters into one unified text block
                String searchHaystack = String.join(" | ",
                        String.valueOf(user.getUserId()),
                        user.getUsername() != null ? user.getUsername() : "",
                        user.getFullName() != null ? user.getFullName() : "",
                        user.getRoleName() != null ? user.getRoleName() : "",
                        user.isIsActive() ? "active" : "inactive",
                        user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
                        user.getLastLogin() != null ? user.getLastLogin().toString() : "never"
                ).toLowerCase();

                // Return true if any part of the concatenated string hits the search phrase
                return searchHaystack.contains(query);
            });
        });
    }

    private void openUserModal(User user) {
        try {
            Config config = Config.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/UserDetailsModal.fxml"));
            loader.setResources(config.getLanguageBundle());
            Parent root = loader.load();
            config.applyOrientation(root);
            Object controller = loader.getController();
            ControllerDispatcher.execute(controller);
            UserDetailsModalController modalController = loader.getController();
            // 🎯 Note: We no longer even need to pass the repository to the modal!
            modalController.setMode(user);

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setScene(new Scene(root));

            // Execution pauses here while the user interacts with the popup
            modalStage.showAndWait();

            // 🌟 THE HANDOFF: Execution resumes here once the window closes
            if (modalController.isSaveClicked()) {

                // Fetch the updated user entity instance directly from the modal's state
                User updatedUser = modalController.getUserChanges();

                // Parent controller securely executes the database write operation
                userRepository.save(updatedUser);

                // Re-fetch from DB and reload the TableView columns instantly
                initializeTable();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }





    private enum FormMode { VIEW_EDIT, CREATION }
    private void setCurrentMode(FormMode mode){ this.currentMode = mode; }
}


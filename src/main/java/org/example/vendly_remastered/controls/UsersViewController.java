package org.example.vendly_remastered.controls;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;

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
    @FXML private TableColumn<User, LocalDateTime> colCreatedAt;
    @FXML private TableColumn<User, LocalDateTime> colLastLogin;

    @FXML private Button addUserButton;
    @FXML private Button userModifyButton;
    @FXML private Button toggleStatusButton;

    private IUserRepository userRepository;
    private ObservableList<User> usersList;

    public void setUserRepository(IUserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // set the language bundle
        // set buttons behaviour
        setTableColumnsBehaviour();
        setAddUserButtonBehaviour();

        // load users data
    }
    public void initializeTable(){
        if (userRepository == null) return;
        // Make sure to instantiate the observable list wrapper
        this.usersList = javafx.collections.FXCollections.observableArrayList();
        List<User> ul = userRepository.findAll();
        if(ul.isEmpty())
            System.out.println("empty list");
        ul.forEach(e -> System.out.println(e.getFullName()));
        this.usersList.addAll(ul);

        userTableView.setItems(this.usersList);
        userTableView.refresh();
    }

    private void setTableColumnsBehaviour(){
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("isActive"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colLastLogin.setCellValueFactory(new PropertyValueFactory<>("lastLogin"));
    }

    private void setAddUserButtonBehaviour(){
        System.out.println("sadsad");
        addUserButton.setOnAction( _ -> {
            initializeTable();
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

    }
    private void setToggleStatusButtonBehviour(){
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
}

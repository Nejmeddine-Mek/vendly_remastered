package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.service.ErrorLogsService;
import org.example.vendly_remastered.utils.ControllerDispatcher;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class LoginViewController implements Initializable {
    @FXML private Label appNameLabel;
    @FXML private Label sloganLabel;
    @FXML private Label usernameLabel;
    @FXML private TextField usernameField;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private IUserRepository userRepository;

    public void setRepository(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // firstly, ome must set any language

        //then set up the default behavior of the login button
        setLoginButtonBehaviour();
    }
    private void setLoginButtonBehaviour(){
        this.loginButton.setOnAction(_ -> {
            String username = this.usernameField.getText().trim();
            String password = this.passwordField.getText();

            handleLogin(username, password);
        });
    }
    private int handleLogin(String username, String plainPassword){
        if (username.isEmpty() || plainPassword.isEmpty()) {
            // Let's assume you have an @FXML private Label errorLabel;
            errorLabel.setText(Config.getInstance().getLanguageBundle().getString("Login.error.emptyFields"));
            errorLabel.setStyle("-fx-text-fill: #ff4d4d;"); // Turn text red
            return 1;
        }
        boolean success = userRepository.authenticate(username, plainPassword);
        if(success){
            try {
                Config config = Config.getInstance();
                // 1. Load the Dashboard FXML file
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/vendly_remastered/MainView.fxml"));
                loader.setResources(config.getLanguageBundle());

                Parent root = loader.load();
                Object controller = loader.getController();
                ControllerDispatcher.execute(controller);



                config.applyOrientation(root);
                // 2. Get the current active Window Stage
                Stage stage = (Stage) loginButton.getScene().getWindow();

                // 3. Switch the scene smoothly
                stage.setScene(new Scene(root));
                stage.setTitle(config.getLanguageBundle().getString("common.appName"));
                stage.centerOnScreen(); // Center the new larger ERP layout
                stage.show();

            } catch (IOException e) {
                String message = "Failed to load layout view component: Main View";
                System.err.println(message);
                ErrorLogsService.getInstance().log(message, e);
                errorLabel.setText(Config.getInstance().getLanguageBundle().getString("Login.error.connectionFailed"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
        this.passwordField.setText("");      // Clear bad password
        this.passwordField.requestFocus();  // Pop cursor back in there instantly

        errorLabel.setText(Config.getInstance().getLanguageBundle().getString("Login.error.invalidCredentials"));
        errorLabel.setStyle("-fx-text-fill: #ff4d4d;");
        return 2;
    }
        return 0;
    }
}

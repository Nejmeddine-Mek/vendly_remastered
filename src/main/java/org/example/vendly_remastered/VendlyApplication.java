package org.example.vendly_remastered;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.example.vendly_remastered.controls.LoginViewController;
import org.example.vendly_remastered.repository.impl.UserSQLiteImpl;
import org.example.vendly_remastered.security.Config;
import org.example.vendly_remastered.utils.DatabaseManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class VendlyApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, SQLException {
        // create config instance
        Config config = Config.getInstance();

        // 1. Create the loader instance
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginView.fxml"));
        loader.setResources(config.getLanguageBundle());

        Parent root = loader.load();
        config.applyOrientation(root);

        Connection dbConnection = DatabaseManager.getConnection();
        // 2. Extract the generated controller from the loader
        LoginViewController controller = loader.getController();

        // 3. CALL IT HERE! Pass the implementation into the slot 🎯
        controller.setRepository(new UserSQLiteImpl(dbConnection));
        Image appIcon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/vendly_remastered_icon.png")));
        stage.getIcons().add(
                appIcon
        );
        // 4. Set the scene and display the window
        stage.setScene(new Scene(root));
        stage.setTitle(Config.getInstance().getLanguageBundle().getString("common.loginWindowTitle"));
        stage.show();
    }
}

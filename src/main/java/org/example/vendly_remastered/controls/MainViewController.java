package org.example.vendly_remastered.controls;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.vendly_remastered.cache.ClientsCache;
import org.example.vendly_remastered.cache.PagesCache;
import org.example.vendly_remastered.cache.ProductsCache;
import org.example.vendly_remastered.constants.Constants;
import org.example.vendly_remastered.constants.PagesPriorities;
import org.example.vendly_remastered.repository.impl.BarcodeSQLImpl;
import org.example.vendly_remastered.repository.impl.ClientsSQLiteImpl;
import org.example.vendly_remastered.repository.impl.ProductSQLiteImpl;
import org.example.vendly_remastered.repository.impl.StockSQLiteImpl;
import org.example.vendly_remastered.security.UserSession;
import org.example.vendly_remastered.utils.Config;
import org.example.vendly_remastered.utils.ControllerDispatcher;
import org.example.vendly_remastered.utils.DatabaseManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private Button salesButton;
    @FXML private Button productsButton;
    @FXML private Button clientsButton;
    @FXML private Button suppliersButton;
    @FXML private Button historyButton;
    @FXML private Button purchasesButton;
    @FXML private Button dashboardButton;
    @FXML private Button settingsButton;
    @FXML private Button usersButton;
    @FXML private Button logoutButton;
    @FXML private Label dateLabel;
    @FXML private Label usernameLabel;

    // Here we have our Logic control variables
    PagesCache pagesCache = new PagesCache();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // check authentications, all together to setup things the right way
        Map<String, Boolean> authorization = UserSession.getInstance().getPrivileges();
        for(Map.Entry<String, Boolean> entry: authorization.entrySet()){
            switch (entry.getKey()){
                case "can_see_clients":
                    clientsButton.setVisible(entry.getValue());
                    break;
                case "can_see_products":
                    productsButton.setVisible(entry.getValue());
                    break;
                case "can_see_suppliers":
                    salesButton.setVisible(entry.getValue());
                    break;
                case "can_see_history":
                    historyButton.setVisible(entry.getValue());
                    break;
                case "can_see_dashboard":
                    dashboardButton.setVisible(entry.getValue());
                    break;
                case "can_see_settings":
                    settingsButton.setVisible(entry.getValue());
                    break;
                case "can_see_users":
                    usersButton.setVisible(entry.getValue());
                    break;
            }
        }

        // now we set default behaviour of each field and button
        try {
            Connection conn = DatabaseManager.getConnection();
            ProductsCache.initialize(20, new ProductSQLiteImpl(conn), new StockSQLiteImpl(conn), new BarcodeSQLImpl(conn));
            ClientsCache.initialize(10,new ClientsSQLiteImpl(conn));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dateLabel.setText(LocalDate.now().toString());
        usernameLabel.setText(UserSession.getInstance().getFullName());
        //now we must inject the default page which should be the sales
        setupNavigationButtons();
        togglePage(Constants.SALES_VIEW);
        //injectView(Constants.getViewPath(Constants.CLIENTS_VIEW));

    }

    private void setupNavigationButtons() {
        // Map each FXML button directly to its corresponding view constant
        salesButton.setOnAction(_ -> togglePage(Constants.SALES_VIEW));
        productsButton.setOnAction(_ -> togglePage(Constants.PRODUCTS_VIEW));
        clientsButton.setOnAction(_ -> togglePage(Constants.CLIENTS_VIEW));
        suppliersButton.setOnAction(_ -> togglePage(Constants.SUPPLIERS_VIEW));
        historyButton.setOnAction(_ -> togglePage(Constants.HISTORY_VIEW));
        purchasesButton.setOnAction(_ -> togglePage(Constants.PURCHASES_VIEW));
        dashboardButton.setOnAction(_ -> togglePage(Constants.DASHBOARD_VIEW));
        settingsButton.setOnAction(_ -> togglePage(Constants.SETTINGS_VIEW));
        usersButton.setOnAction(_ -> togglePage(Constants.USERS_VIEW));
    }

    private void togglePage(Constants pageName) {
        // 1. Look for the page in your 4-slot cache
        Parent page = pagesCache.getPage(pageName);

        if (page == null) {
            // CACHE MISS: Get the FXML path
            String fxmlPath = Constants.getViewPath(pageName);

            // Load the page and apply your custom layout fixes
            page = injectView(fxmlPath);

            // Grab its priority and store it in the cache engine
            int priority = PagesPriorities.getPriority(pageName);
            pagesCache.addPage(page, pageName, priority);
            System.out.println("[Router] Cache MISS. Loaded and cached: " + pageName);
        } else {
            System.out.println("[Router] Cache HIT! Reusing layout for: " + pageName);
        }

        // 2. Clear old screen elements and drop the hot page into view
        contentContainer.getChildren().clear();
        contentContainer.getChildren().add(page);
    }
    private Parent injectView(String fxmlPath) {
        try {
            Config config = Config.getInstance();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setResources(config.getLanguageBundle());

            Parent targetNode = loader.load();
            config.applyOrientation(targetNode);
            Object controller = loader.getController();
            if(controller != null)
                ControllerDispatcher.execute(controller);

            // inject around here maybe
            // 1. Force your custom layout constraints
            VBox.setVgrow(targetNode, javafx.scene.layout.Priority.ALWAYS);

            // 2. Your Forced Dimension Fix (Keeps UI responsive)
            if (targetNode instanceof Region region) {
                region.prefWidthProperty().bind(contentContainer.widthProperty());
                region.prefHeightProperty().bind(contentContainer.heightProperty());
            }

            // 3. AnchorPane fallback rules
            if (targetNode instanceof AnchorPane) {
                AnchorPane.setTopAnchor(targetNode, 0.0);
                AnchorPane.setBottomAnchor(targetNode, 0.0);
                AnchorPane.setLeftAnchor(targetNode, 0.0);
                AnchorPane.setRightAnchor(targetNode, 0.0);
            }

            // Return the perfectly scaled layout root so togglePage can cache it
            return targetNode;

        } catch (IOException e) {
            System.err.println("Failed to load layout view component: " + fxmlPath);
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

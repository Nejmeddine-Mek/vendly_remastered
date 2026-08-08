package org.example.vendly_remastered.controls;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.vendly_remastered.models.entity.Batch;
import org.example.vendly_remastered.security.Config;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class ExpiryAlertModalController implements Initializable {
    @FXML private TableView<Batch> tableExpiryAlerts;

    @FXML private TableColumn<Batch, String> colProductName;

    @FXML private TableColumn<Batch, LocalDate> colExpiryDate;

    @FXML private TableColumn<Batch, String> colDaysRemaining;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setTableColumnsBehaviour();
    }

    private void setTableColumnsBehaviour() {
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colExpiryDate.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));

        colDaysRemaining.setCellValueFactory(cellData -> {
            LocalDate expiryDate = cellData.getValue().getExpiryDate();
            if (expiryDate == null) {
                return new SimpleStringProperty("");
            }

            // Example: Convert LocalDate directly to a string format (e.g., "2026-08-11")
            return new SimpleStringProperty(expiryDate.toString());
        });
        colDaysRemaining.setCellFactory(column -> new TableCell<Batch, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Batch batch = getTableRow().getItem();
                    LocalDate expiryDate = batch.getExpiryDate();

                    if (expiryDate == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);

                    if (daysLeft < 0) {
                        setText(Config.getInstance().getLanguageBundle().getString("expiry.status.expired"));
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else if (daysLeft == 0) {
                        setText(Config.getInstance().getLanguageBundle().getString("expiry.status.expired"));
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                    } else {
                        String format = Config.getInstance().getLanguageBundle().getString("expiry.status.days_count");
                        setText(String.format(format, daysLeft));
                        setStyle("-fx-text-fill: #10B981;");
                    }
                }
            }
        });    }

    public void setMode(List<Batch> expectedExpires){
        ObservableList<Batch> expiringItems = FXCollections.observableArrayList(expectedExpires);
        tableExpiryAlerts.setItems(expiringItems);
        tableExpiryAlerts.refresh();
    }
}

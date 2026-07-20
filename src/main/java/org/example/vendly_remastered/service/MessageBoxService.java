package org.example.vendly_remastered.service;

import javafx.scene.control.Alert;

public class MessageBoxService {
    public static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

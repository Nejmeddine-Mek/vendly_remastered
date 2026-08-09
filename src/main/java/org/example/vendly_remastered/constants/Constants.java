package org.example.vendly_remastered.constants;

public enum Constants {
    SALES_VIEW, CLIENTS_VIEW, SUPPLIERS_VIEW,
    PRODUCTS_VIEW, HISTORY_VIEW, MAIN_VIEW,
    LOGIN_VIEW, SETTINGS_VIEW, USERS_VIEW,
    DASHBOARD_VIEW, PURCHASES_VIEW;

    public static String getViewPath(Constants constant) {
        String path = switch (constant) {
            case SALES_VIEW     -> "SalesView.fxml";
            case CLIENTS_VIEW   -> "ClientsView.fxml";
            case SUPPLIERS_VIEW -> "suppliersView.fxml";
            case PRODUCTS_VIEW  -> "productsView.fxml";
            case HISTORY_VIEW   -> "HistoryView.fxml";
            case MAIN_VIEW      -> "MainView.fxml";
            case LOGIN_VIEW     -> "LoginView.fxml";
            case SETTINGS_VIEW  -> "SettingsView.fxml";
            case USERS_VIEW     -> "UsersView.fxml";
            case DASHBOARD_VIEW -> "DashboardView.fxml";
            case PURCHASES_VIEW -> "PurchasesHistoryView.fxml";
        };

        return "/org/example/vendly_remastered/" + path;
    }
}

package org.example.vendly_remastered.constants;

import org.example.vendly_remastered.constants.Constants;

public class PagesPriorities {

    public static int getPriority(Constants pageName) {
        return switch (pageName) {
            case SALES_VIEW      -> 1; // Absolute highest protection
            case PRODUCTS_VIEW   -> 2; // Heavy UI components
            case DASHBOARD_VIEW  -> 3; // Analytical tier
            case HISTORY_VIEW    -> 3; // Log data tables
            case PURCHASES_VIEW  -> 4; // Operational restock forms
            case CLIENTS_VIEW    -> 5; // Registry tiers
            case SUPPLIERS_VIEW  -> 5; // Registry tiers
            case SETTINGS_VIEW   -> 6; // Low frequency administration
            case USERS_VIEW      -> 6; // Low frequency administration
            case MAIN_VIEW,
                 LOGIN_VIEW      -> 6; // Basic structural/transient views
        };
    }
}
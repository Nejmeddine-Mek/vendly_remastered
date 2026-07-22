package org.example.vendly_remastered.utils;

import org.example.vendly_remastered.controls.*;
import org.example.vendly_remastered.repository.impl.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ControllerDispatcher {

    // 🎯 The registry map that shifts lookup complexity from O(N) down to O(1)
    private static final Map<Class<?>, ControllerInitializer> registry = new HashMap<>();

    static {
        // Register UsersViewController initializer rules
        registry.put(UsersViewController.class, controller -> {
            Connection conn = DatabaseManager.getConnection();
            UsersViewController uvc = (UsersViewController) controller;
            uvc.setUserRepository(new UserSQLiteImpl(conn));
            uvc.initializeTable();
        });

        // Register UserDetailsModalController initializer rules
        registry.put(UserDetailsModalController.class, controller -> {
            Connection conn = DatabaseManager.getConnection();
            UserDetailsModalController udmc = (UserDetailsModalController) controller;
            udmc.setRoleRepository(new RoleSQLiteImpl(conn));
            udmc.setComboRoleValues();
        });

        registry.put(HistoryViewController.class, controller -> {
           Connection conn = DatabaseManager.getConnection();
            HistoryViewController hvc = (HistoryViewController) controller;
            hvc.setSalesHeaderRepository(new SaleHeaderSQLiteImpl(conn));
            hvc.setSaleDetailsRepository(new SaleDetailsSQLiteImpl(conn));
            hvc.loadInitialSalesTableData();

        });
        registry.put(ProductsViewController.class, controller -> {
            Connection conn = DatabaseManager.getConnection();
            ProductsViewController pvc = (ProductsViewController) controller;
            pvc.setProductRepository(new ProductSQLiteImpl(conn));
            pvc.setCategoryRepository(new CategorySQLiteImpl(conn));
            pvc.setStockRepository(new StockSQLiteImpl(conn));
            pvc.loadCategories();
            pvc.initializeTable();
        });
        registry.put(ProductModalController.class, controller -> {
           Connection conn = DatabaseManager.getConnection();
           ProductModalController pmc = (ProductModalController) controller;
           pmc.setCategoryRepository(new CategorySQLiteImpl(conn));
           pmc.setBarcodeRepository(new BarcodeSQLImpl(conn));
           pmc.setComboCategoryValues();
        });
        registry.put(SalesViewController.class, controller -> {
            Connection conn = DatabaseManager.getConnection();
            SalesViewController svc = (SalesViewController) controller;
            svc.setSaleHeaderRepository(new SaleHeaderSQLiteImpl(conn));
            svc.setSaleDetailsRepository(new SaleDetailsSQLiteImpl(conn));
        });
        registry.put(ClientsViewController.class, controller -> {
            Connection conn = DatabaseManager.getConnection();
            ClientsViewController cvc = (ClientsViewController) controller;
            cvc.setClientsRepository(new ClientsSQLiteImpl(conn));
            cvc.initializeTable();

        });
        registry.put(ClientDetailsModalController.class, controller -> {

        });
        // 🌟 Adding a new controller next week? Just drop another registry.put() right here!
    }

    public static void execute(Object controller) throws SQLException {
        if (controller == null) return;

        // Get the specific code block for this exact class type in O(1) constant time
        ControllerInitializer initializer = registry.get(controller.getClass());

        if (initializer != null) {
            initializer.initialize(controller);
        } else {
            // Optional: fallback warning if an unregistered controller passes through
            System.out.println("Warning: No initializer registered for " + controller.getClass().getSimpleName());
        }
    }
}
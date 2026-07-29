package org.example.vendly_remastered;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        //DatabaseManager.initializeDatabase();
        //String res = PasswordHasher.hashPassword("admin");
        //System.out.println(res);
        Application.launch(VendlyApplication.class, args);
    }
}

package org.example.vendly_remastered;

import javafx.application.Application;
import org.example.vendly_remastered.security.PasswordHasher;
import org.example.vendly_remastered.utils.Config;
import org.example.vendly_remastered.utils.DatabaseManager;

public class Launcher {
    public static void main(String[] args) {
        //DatabaseManager.initializeDatabase();
        //String res = PasswordHasher.hashPassword("admin");
        //System.out.println(res);
        Application.launch(VendlyApplication.class, args);
    }
}

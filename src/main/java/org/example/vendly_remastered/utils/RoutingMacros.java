package org.example.vendly_remastered.utils;

import org.example.vendly_remastered.controls.UsersViewController;
import org.example.vendly_remastered.repository.IUserRepository;
import org.example.vendly_remastered.repository.impl.UserSQLiteImpl;

import java.sql.SQLException;

public class RoutingMacros {
    public static void execute(Object controller) throws SQLException {
        if(controller instanceof UsersViewController){
            ((UsersViewController) controller).setUserRepository(new UserSQLiteImpl(DatabaseManager.getConnection()));
            ((UsersViewController) controller).initializeTable();
        }
    }
}
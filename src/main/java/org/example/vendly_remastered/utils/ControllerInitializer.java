package org.example.vendly_remastered.utils;

import java.sql.SQLException;

@FunctionalInterface
public interface ControllerInitializer {

    void initialize(Object controller) throws SQLException;

}

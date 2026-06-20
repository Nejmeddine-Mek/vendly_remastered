package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.User;
import org.example.vendly_remastered.repository.IUserRepository;

import java.sql.Connection;
import java.util.Optional;

public class UserSQLiteImpl implements IUserRepository {
    private final Connection connection;
    public UserSQLiteImpl(Connection connection){
        this.connection = connection;
    }
    @Override
    public boolean authenticate() {
        return false;
    }

    @Override
    public Optional<User> searchByUsername() {
        return Optional.empty();
    }
}

package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.User;

import java.util.Optional;

public interface IUserRepository {
    public boolean authenticate(String username, String password);
    public Optional<User> searchByUsername(String username);
}

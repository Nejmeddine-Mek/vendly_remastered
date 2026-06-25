package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.repository.IClientsRepository;

import java.sql.Connection;
import java.util.List;

public class ClientsSQLiteImpl implements IClientsRepository {
    private Connection connection;
    public ClientsSQLiteImpl(Connection conn){ connection = conn; }

    @Override
    public boolean add(Client client) {
        return false;
    }

    @Override
    public boolean update(Client client) {
        return false;
    }

    @Override
    public boolean delete(int clientId) {
        return false;
    }

    @Override
    public List<Client> findAll() {
        return List.of();
    }
}

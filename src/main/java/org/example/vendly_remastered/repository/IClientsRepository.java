package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Client;

import java.util.List;

public interface IClientsRepository {
    public boolean add(Client client);
    public boolean update(Client client);
    public boolean delete(int clientId);
    public List<Client> findAll();

}

package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Client;

import java.util.List;
import java.util.Optional;

public interface IClientsRepository {
    public List<Client> getByName(String name);
    public Optional<Client> get(int clientId);
    public boolean add(Client client);
    public boolean update(Client client);
    public boolean delete(int clientId);
    public List<Client> findAll();

}

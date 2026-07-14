package org.example.vendly_remastered.cache.model;

import org.example.vendly_remastered.models.entity.Client;

public class ClientCacheLine {
    private long clientCacheCounter;
    private Client client;
    public ClientCacheLine(long cacheCounter, Client client){
        this.clientCacheCounter = cacheCounter;
        this.client = client;
    }

    public long getClientCacheCounter() {
        return clientCacheCounter;
    }

    public void setClientCacheCounter(long clientCacheCounter) {
        this.clientCacheCounter = clientCacheCounter;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}

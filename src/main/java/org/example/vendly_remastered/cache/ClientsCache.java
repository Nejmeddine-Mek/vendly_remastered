package org.example.vendly_remastered.cache;

import org.example.vendly_remastered.cache.model.ClientCacheLine;
import org.example.vendly_remastered.models.entity.Client;

import java.util.HashMap;

public class ClientsCache {
    private static ClientsCache instance;
    //private final ClientRepository clientRepository; // Database layer access

    // 🛠️ Your structural state variables
    private long Counter;
    private ClientCacheLine[] cache;
    private HashMap<String, Integer> clientNameIdIndex;

    private static final int CACHE_SIZE = 50;
    // Private constructor initializing indices and size dimensions
    private ClientsCache() {
        this.cache = new ClientCacheLine[CACHE_SIZE];
        this.clientNameIdIndex = new HashMap<>();
        //this.clientRepository = new ClientRepository(); // Initialize your repository database hook
        this.Counter = 0;
    }

    public static synchronized ClientsCache getInstance() {
        if (instance == null) {
            instance = new ClientsCache();
        }
        return instance;
    }
    /**
     * 🟢 PUT (Write-Through Implementation)
     * Persists straight down to the database first, then populates the cache matrix.
     */
    public synchronized void put(Client client) {
        if (client == null) return;
/*
        try {
            // 1. Write-Through: Insert/Update SQLite database immediately
            boolean dbSuccess = clientRepository.saveOrUpdate(client);
            if (!dbSuccess) {
                System.err.println("[ClientsCache] DB Write aborted. Cache sync skipped to prevent corrupted states.");
                return;
            }

            Counter++;
            int targetIndex = -1;

            // 2. Check if the client is already mapped inside a cache line
            for (int i = 0; i < cache.length; i++) {
                if (cache[i] != null && cache[i].client.getClientId() == client.getClientId()) {
                    targetIndex = i;
                    break;
                }
            }

            // 3. LRU Eviction Logic: If it's a new entry and cache array is full, evict the coldest line
            if (targetIndex == -1) {
                int emptyIndex = -1;
                int coldestIndex = 0;
                long oldestTimestamp = Long.MAX_VALUE;

                for (int i = 0; i < cache.length; i++) {
                    if (cache[i] == null) {
                        emptyIndex = i;
                        break;
                    }
                    if (cache[i].lastAccessed < oldestTimestamp) {
                        oldestTimestamp = cache[i].lastAccessed;
                        coldestIndex = i;
                    }
                }

                // Prioritize an empty slot; otherwise, overwrite the coldest entry line
                targetIndex = (emptyIndex != -1) ? emptyIndex : coldestIndex;

                // Evict the stale record pointer from our lookup name index map
                if (cache[targetIndex] != null) {
                    clientNameIdIndex.remove(cache[targetIndex].client.getClientName().toLowerCase());
                }
                // 4. Update the cache line arrays and hash tracking indexes
            cache[targetIndex] = new ClientCacheLine(client, Counter);
            clientNameIdIndex.put(client.getClientName().toLowerCase(), client.getClientId());

        } catch (Exception e) {
            System.err.println("[ClientsCache] Unexpected error executing structural write-through.");
            e.printStackTrace();
        }
    }
 */
            }
    public synchronized Client get(int clientId) {
        Counter++;
        // Check cache lines first
        for (ClientCacheLine line : cache) {
            if (line != null && line.getClient().getClientId() == clientId) {
                line.setClientCacheCounter(Counter); // Mark line as hot
                return line.getClient();
            }
        }

        // Cache Miss: Query DB, then quietly load into a cache line
        /*Client client = clientRepository.findById(clientId);
        if (client != null) {
            putQuietly(client);
        }*/
        return client;
    }
    public synchronized Client getByName(String clientName) {
        if (clientName == null || clientName.isBlank()) return null;

        Integer matchedId = clientNameIdIndex.get(clientName.trim().toLowerCase());
        if (matchedId != null) {
            return get(matchedId); // Loops back to standard GET to update hot tags
        }

        // Cache Miss: Query database name rows directly
        Client client = clientRepository.findByName(clientName.trim());
        if (client != null) {
            putQuietly(client);
        }
        return client;
    }
    private void putQuietly(Client client) {
        Counter++;
        int emptyIndex = -1;
        int coldestIndex = 0;
        long oldestTimestamp = Long.MAX_VALUE;

        for (int i = 0; i < cache.length; i++) {
            if (cache[i] == null) {
                emptyIndex = i;
                break;
            }
            if (cache[i].lastAccessed < oldestTimestamp) {
                oldestTimestamp = cache[i].lastAccessed;
                coldestIndex = i;
            }
        }

        int targetIndex = (emptyIndex != -1) ? emptyIndex : coldestIndex;
        if (cache[targetIndex] != null) {
            clientNameIdIndex.remove(cache[targetIndex].client.getClientName().toLowerCase());
        }

        cache[targetIndex] = new ClientCacheLine(client, Counter);
        clientNameIdIndex.put(client.getClientName().toLowerCase(), client.getClientId());
    }


}

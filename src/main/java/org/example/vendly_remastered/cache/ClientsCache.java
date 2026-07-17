package org.example.vendly_remastered.cache;

import org.example.vendly_remastered.cache.model.ClientCacheLine;
import org.example.vendly_remastered.models.entity.Client;
import org.example.vendly_remastered.repository.IClientsRepository;
import org.example.vendly_remastered.repository.impl.ClientsSQLiteImpl;
import org.example.vendly_remastered.utils.DatabaseManager;

import java.sql.SQLException;
import java.util.*;

public class ClientsCache {

    // Thread-safe global active instance (volatile for double-checked locking safety)
    private static volatile ClientsCache instance;

    // Monotonically increasing counter to stamp access mutations (or logical clocks)
    private long cacheVersionCounter;
    private final int maxCacheSize;

    // Core database gateway
    private final IClientsRepository clientsRepository;

    // High-performance LinkedHashMap configured for Access-Order tracking (LRU eviction)
    private final LinkedHashMap<Integer, ClientCacheLine> cacheMap;
    private final HashMap<String, Integer> clientNameIdIndex;

    /**
     * Private Constructor: Configured to mirror ProductsCache initialization exactly.
     */
    private ClientsCache(int capacity, IClientsRepository repository) {
        this.maxCacheSize = capacity;
        this.clientsRepository = repository;
        this.cacheVersionCounter = 0;
        this.clientNameIdIndex = new HashMap<>();

        // Instantiate LinkedHashMap configured for auto-LRU eviction based on access order
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ClientCacheLine> eldest) {
                if (size() > maxCacheSize) {
                    ClientCacheLine eldestLine = eldest.getValue();
                    Client client = eldestLine.getClient();

                    // Clean name map indexes to prevent memory leaks on eviction
                    if (client != null && client.getContactFullName() != null) {
                        clientNameIdIndex.remove(client.getContactFullName().trim().toLowerCase());
                    }
                    if (client != null && client.getCompanyName() != null) {
                        clientNameIdIndex.remove(client.getCompanyName().trim().toLowerCase());
                    }
                    return true; // Authorize the structural eviction from cacheMap
                }
                return false;
            }
        };
    }

    /**
     * Initializes the global cache instance at application startup.
     */
    public static synchronized ClientsCache initialize(int capacity, IClientsRepository repository) {
        if (instance == null) {
            instance = new ClientsCache(capacity, repository);
        }
        return instance;
    }

    /**
     * Retrieves the active global instance.
     */
    public static ClientsCache getInstance() {
        if (instance == null) {
            try {
                // Fail-safe initialization to prevent runtime startup exceptions if not pre-initialized
                System.out.println("[ClientsCache] Warning: Auto-initializing default SQLite repository fallback.");
                instance = new ClientsCache(50, new ClientsSQLiteImpl(DatabaseManager.getConnection()));
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to fallback-initialize ClientsCache database handle.", e);
            }
        }
        return instance;
    }

    /**
     * 🔵 GET by ID (Memory Hit -> DB Miss Fallback)
     */
    public synchronized Client get(int clientId) {
        ClientCacheLine line = this.cacheMap.get(clientId);

        if (line != null) {
            this.cacheVersionCounter++;
            line.setClientCacheCounter(this.cacheVersionCounter); // Mark line as hot
            System.out.println("[ClientsCache] HIT for Client ID: " + clientId);
            return line.getClient();
        }

        System.out.println("[ClientsCache] MISS for Client ID: " + clientId + " -> Querying SQLite storage.");
        Optional<Client> clientOpt = clientsRepository.get(clientId);

        if (clientOpt.isPresent()) {
            Client client = clientOpt.get();
            this.putQuietly(client); // Load directly without executing write-through triggers
            return client;
        }

        return null;
    }

    /**
     * 🔵 GET by Name (Case-insensitive secondary index mapping lookup)
     */
    public synchronized Client getByName(String clientName) {
        if (clientName == null || clientName.isBlank()) return null;

        Integer clientId = clientNameIdIndex.get(clientName.trim().toLowerCase());
        if (clientId != null) {
            return get(clientId); // Redirects into get() to elevate cache hit access status
        }

        // Cache Miss fallback search via SQLite implementation
        System.out.println("[ClientsCache] MISS for Name: " + clientName + " -> Checking persistent DB layer.");

        // Note: Replace with your repository's actual by-name lookup method if named differently
        List<Client> clientOpt = clientsRepository.getByName(clientName.trim());
        // TODO: requires special way to treat it.
        if(clientOpt.size() == 1){
            put(clientOpt.getFirst());
            return clientOpt.getFirst();
        } else if (clientOpt.size() > 1){
            for(Client c : clientOpt){
                put(c);
            }
            Client c = new Client();
            c.setContactFullName("RollbackOption");
            //TRIGGER THE SUGGESTION AGAIN, MULTIPLE OPTIONS FOUND
            return c;
        }
        // MEANS CLIENT DOES NOT EXIST
        return null;
    }

    /**
     * 🟢 PUT (Write-Through Implementation)
     * Persists straight down to the database synchronously, then maps it in memory.
     */
    public synchronized void put(Client client) {
        if (client == null) return;

        try {
            // Write-Through: Immediately attempt database write transaction
            boolean dbSuccess = clientsRepository.add(client);
            if (!dbSuccess) {
                System.err.println("[ClientsCache] Write-Through aborted: Repository failed database insertion.");
                return;
            }

            // Sync successfully written record back into cache mapping layers
            this.putQuietly(client);

        } catch (Exception e) {
            System.err.println("[ClientsCache] CRITICAL: Unexpected error on Write-Through persistence loop.");
            e.printStackTrace();
        }
    }

    /**
     * Quietly adds or updates elements in memory during cache-miss queries
     * without creating duplicate database write operations.
     */
    private void putQuietly(Client client) {
        this.cacheVersionCounter++;

        ClientCacheLine line = new ClientCacheLine(this.cacheVersionCounter, client);
        this.cacheMap.put(client.getClientId(), line);

        // Map personal full name index
        if (client.getContactFullName() != null) {
            this.clientNameIdIndex.put(client.getContactFullName().trim().toLowerCase(), client.getClientId());
        }

        // Map company name index (if exists)
        if (client.getCompanyName() != null && !client.getCompanyName().isBlank()) {
            this.clientNameIdIndex.put(client.getCompanyName().trim().toLowerCase(), client.getClientId());
        }
    }

    /**
     * Manual Cache Invalidation
     */

    public long getCacheVersionCounter() {
        return cacheVersionCounter;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public synchronized Collection<ClientCacheLine> getCacheMapValues() {
        return this.cacheMap.values();
    }
}
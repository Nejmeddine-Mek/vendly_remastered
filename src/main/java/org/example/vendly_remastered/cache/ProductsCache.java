package org.example.vendly_remastered.cache;

import org.example.vendly_remastered.cache.model.ProductCacheLine;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ProductsCache {
    // Monotonically increasing counter to stamp mutations (our LSN equivalent)
    private long cacheVersionCounter;

    // Strict threshold capacity representing 15% of a 10,000 product inventory
    private final int maxCacheSize;

    // Core database gateway to handle cache misses and dirty line flushes
    private final IProductRepository repository;

    // High-performance mapping grid configured specifically for Access-Order (LRU tracking)
    private final LinkedHashMap<Integer, ProductCacheLine> cacheMap;
    private final HashMap<String, Integer> barcodeToIdIndex = new HashMap<>();
    private final HashMap<String, Integer> nameToIdIndex = new HashMap<>();


    /**
     * Initializes the bounded write-back cache instance.
     * @param capacity The strict maximum number of lines allowed resident in memory (e.g., 1500).
     * @param repo The persistent repository subsystem connection.
     */
    public ProductsCache(int capacity, IProductRepository repo) {
        this.maxCacheSize = capacity;
        this.repository = repo;
        this.cacheVersionCounter = 0;

        // Instantiate LinkedHashMap configured for LRU tracking
        this.cacheMap = new LinkedHashMap<Integer, ProductCacheLine>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ProductCacheLine> eldest) {
                // Check if the cache footprint has spilled past your maximum constraint line
                if (size() > maxCacheSize) {
                    ProductCacheLine eldestLine = eldest.getValue();
                    Product product = eldestLine.getProduct();

                    // 1. Transactional Write-Back: Persist if the cache line state is dirty
                    if (eldestLine.isDirtyBit()) {
                        flushToDisk(eldestLine);
                    }

                    // 2. Index Clean: Scrub pointer references to eliminate stale tracking memory leaks
                    nameToIdIndex.remove(product.getProductName());
                    if (product.getBarcodes() != null) {
                        for (String barcode : product.getBarcodes()) {
                            barcodeToIdIndex.remove(barcode);
                        }
                    }

                    // Return true to authorize the structural removal of this entry from cacheMap
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Retrieves a product from memory (Cache Hit) or falls back to storage (Cache Miss).
     * Moving the accessed line to the head of the LRU timeline.
     * * @param productId The lookup primary key.
     * @return The populated Product entity, or null if it doesn't exist in the system.
     */
    public synchronized Product get(int productId) {
        // 1. Safe Lookup: Get the cache line wrapper first without unpacking it immediately
        assert this.cacheMap != null;
        ProductCacheLine line = this.cacheMap.get(productId);

        if (line != null) {
            System.out.println("Cache HIT for Product ID: " + productId);
            return line.getProduct(); // Pure RAM delivery
        }

        // 2. Cache MISS: Safely drop down to your repository layer
        System.out.println("Cache MISS for Product ID: " + productId + " -> Querying DB.");
        Product product = repository.findProduct(productId).orElse(null);

        // 3. Absolute Miss: If it's not even in the database, return null
        if (product == null) {
            return null;
        }

        // 4. Allocation: Let your unified put() method handle the index wiring seamlessly
        this.put(product);

        return product;
    }

    public synchronized Product getByBarcode(String barcode) {
        // Step A: Check our secondary pointer index
        Integer productId = barcodeToIdIndex.get(barcode);

        if (productId != null) {
            // Cache Hit! Route straight to the master cache line
            return get(productId);
        }

        // Cache Miss! Drop to DB using your explicit repo method
        Optional<Product> productOpt = repository.findByBarCode(barcode);
        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            put(p); // Injects into master cache and wires up the secondary maps
            return p;
        }

        return null; // Not found anywhere
    }




    /**
     * Writes or modifies a product entry inside the cache grid.
     * Increments the cacheVersionCounter, updates the target slot, and flags the dirty bit.
     * * @param product The updated or newly constructed product data instance.
     */
    /**
     * Master put method with explicit control over which tables are dirty.
     */
    public synchronized void put(Product product, boolean productDirty, boolean stockDirty, boolean barcodeDirty) {
        // 1. Defend against null pointer references
        if (product == null) return;

        // 2. Increment our global sequence counter (our volatile LSN equivalent)
        this.cacheVersionCounter++;

        // 3. Instantiate the line wrapper with explicit fine-grained dirty states
        ProductCacheLine line = new ProductCacheLine(
                product,
                productDirty,
                stockDirty,
                barcodeDirty,
                this.cacheVersionCounter
        );

        // 4. Update the Master Cache Map Registry
        this.cacheMap.put(product.getProductId(), line);

        // 5. Update the primary indexing lookup routes
        if (product.getProductName() != null) {
            this.nameToIdIndex.put(product.getProductName(), product.getProductId());
        }

        // Handle the multi-barcode collection safely
        if (product.getBarcodes() != null) {
            for (String barcode : product.getBarcodes()) {
                if (barcode != null) {
                    this.barcodeToIdIndex.put(barcode, product.getProductId());
                }
            }
        }
    }

    /**
     * Overloaded convenience method.
     * Treats a standard put as a full write/update (all tables marked dirty).
     */
    public synchronized void put(Product product) {
        this.put(product, true, true, true);
    }

    /**
     * Forces an immediate, synchronous persistent write-back of a specific entry to disk.
     * Clears the dirty status indicator flag upon success.
     * * @param line The specific memory cache line to synchronize.
     */
    private void flushToDisk(ProductCacheLine line) {
        // TODO: Pass line payload to repository.update() or save(), toggle dirtyBit to false
        repository.save(line.getProduct());
        //line.setDirtyBits(false);
    }

    /**
     * Sweeps the entire memory map to flush any remaining uncommitted dirty data rows.
     * Typically executed during a graceful shutdown sequence or standard checkpoint intervals.
     */
    public synchronized void flushAll() {
        System.out.println("Executing system checkpoint: Sweeping all dirty cache lines...");

        // Sweep the entire active memory map
        for (ProductCacheLine line : cacheMap.values()) {
            if (line.isDirtyBit()) {
                flushToDisk(line); // Updates DB AND clears the dirty bit status flags
            }
        }

        System.out.println("System checkpoint completed successfully.");
    }

    /**
     * Explicitly purges an entry from the tracking index.
     * If the target row state is dirty, it must flush to storage prior to memory eviction.
     * * @param productId The target resource index identifier to evict.
     */
    public synchronized void invalidate(int productId) {
        // 1. Pull the line from the master map immediately
        ProductCacheLine line = cacheMap.remove(productId);

        // If it wasn't in the cache to begin with, we are done
        if (line == null) return;

        // 2. ONLY write to disk if there are uncommitted changes (Dirty)
        if (line.isDirtyBit()) {
            System.out.println("Invalidation Safeguard: Flushing dirty line to disk before removal.");
            flushToDisk(line);
        }

        // 3. Clear out secondary index references to prevent memory leaks
        Product p = line.getProduct();
        this.nameToIdIndex.remove(p.getProductName());

        if (p.getBarcodes() != null) {
            for (String barcode : p.getBarcodes()) {
                this.barcodeToIdIndex.remove(barcode);
            }
        }

        System.out.println("Successfully evicted product ID " + productId + " from all cache layers.");
    }

    // ==========================================================================
    // GETTERS & SETTERS (Diagnostic and Operational access)
    // ==========================================================================

    public long getCacheVersionCounter() {
        return cacheVersionCounter;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

}
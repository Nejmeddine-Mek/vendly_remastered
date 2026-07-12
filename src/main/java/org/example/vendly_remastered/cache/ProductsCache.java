package org.example.vendly_remastered.cache;

import org.example.vendly_remastered.cache.model.ProductCacheLine;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;
import org.example.vendly_remastered.repository.IStockRepository;

import java.util.*;

public class ProductsCache {

    // The single thread-safe global instance (volatile for safe double-checked locking if needed)
    private static volatile ProductsCache instance;

    // Monotonically increasing counter to stamp mutations (our LSN equivalent)
    private long cacheVersionCounter;

    // Strict threshold capacity representing 15% of a 10,000 product inventory
    private final int maxCacheSize;

    // Core database gateways to handle cache misses and dirty line flushes
    private final IProductRepository productRepository;
    private final IStockRepository stockRepository;

    // High-performance mapping grid configured specifically for Access-Order (LRU tracking)
    private final LinkedHashMap<Integer, ProductCacheLine> cacheMap;
    private final HashMap<String, Integer> barcodeToIdIndex = new HashMap<>();
    private final HashMap<String, Integer> nameToIdIndex = new HashMap<>();

    /**
     * Private Constructor: Prevents direct external instantiation.
     */
    private ProductsCache(int capacity, IProductRepository repo, IStockRepository stockRepository) {
        this.maxCacheSize = capacity;
        this.productRepository = repo;
        this.stockRepository = stockRepository;
        this.cacheVersionCounter = 0;

        // Instantiate LinkedHashMap configured for LRU tracking
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ProductCacheLine> eldest) {
                if (size() > maxCacheSize) {
                    ProductCacheLine eldestLine = eldest.getValue();
                    Product product = eldestLine.getProduct();

                    // 1. Transactional Write-Back: Persist if any bits are dirty
                    if (eldestLine.isDirtyBit()) {
                        flushToDisk(eldestLine);
                    }

                    // 2. Index Clean: Scrub pointer references to eliminate stale tracking memory leaks
                    if (product.getProductName() != null) {
                        nameToIdIndex.remove(product.getProductName());
                    }
                    if (product.getBarcodes() != null) {
                        for (String barcode : product.getBarcodes()) {
                            if (barcode != null) {
                                barcodeToIdIndex.remove(barcode);
                            }
                        }
                    }
                    return true; // Authorize the structural eviction from cacheMap
                }
                return false;
            }
        };
    }

    /**
     * Initializes the global cache instance. Must be called once during app startup.
     */
    public static synchronized ProductsCache initialize(int capacity, IProductRepository repo, IStockRepository stockRepository) {
        if (instance == null) {
            instance = new ProductsCache(capacity, repo, stockRepository);
        }
        return instance;
    }

    /**
     * Retrieves the global active cache registry.
     */
    public static ProductsCache getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ProductsCache has not been initialized! Call initialize() first at startup.");
        }
        return instance;
    }

    /**
     * Retrieves a product from memory (Cache Hit) or falls back to storage (Cache Miss).
     */
    public synchronized Product get(int productId) {
        ProductCacheLine line = this.cacheMap.get(productId);

        if (line != null) {
            System.out.println("Cache HIT for Product ID: " + productId);
            return line.getProduct();
        }

        System.out.println("Cache MISS for Product ID: " + productId + " -> Querying DB.");
        Product product = productRepository.findProduct(productId).orElse(null);

        if (product == null) {
            return null;
        }

        // Pass false flags across all dirty parameters since this is an unmutated read allocation
        this.put(product, false, false, false);
        return product;
    }

    /**
     * Secondary index lookups by Barcode string pattern.
     */
    public synchronized Product getByBarcode(String barcode) {
        Integer productId = barcodeToIdIndex.get(barcode);

        if (productId != null) {
            return get(productId); // Dynamic hit elevation inside core get()
        }

        System.out.println("Cache MISS for Barcode: " + barcode + " -> Querying DB.");
        Optional<Product> productOpt = productRepository.findByBarCode(barcode);
        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            this.put(p, false, false, false);
            return p;
        }

        return null;
    }

    /**
     * In-memory lookup matching the exact name string.
     * Bypasses database layers entirely.
     */
    public synchronized Product getByName(String name) {
        if (name == null || name.isBlank()) return null;

        // Query our secondary name mapping layer
        Integer productId = nameToIdIndex.get(name.trim().toLowerCase());

        if (productId != null) {
            ProductCacheLine line = cacheMap.get(productId);
            if (line != null) {
                return line.getProduct(); // Pure memory delivery
            }
        }
        List<Product> res = productRepository.findByName(name);
        if(!res.isEmpty()){
            for(Product p : res){
                put(p);
            }
            Product p = new Product();
            p.setProductName("rollbackState");
            return p;
        }
        return null; // Leave the case of miss empty
    }

    /**
     * Sweeps internal memory structures for partial name query criteria matching.
     * Optimized specifically for generating fast UI auto-suggestions.
     */
    public synchronized List<Product> searchByName(String name) {
        List<Product> matches = new java.util.ArrayList<>();
        if (name == null || name.isBlank()) return matches;

        String query = name.trim().toLowerCase();

        // Sweep the active cache mapping pool directly
        for (ProductCacheLine line : cacheMap.values()) {
            Product product = line.getProduct();
            if (product != null && product.getProductName() != null) {
                // Check for partial or structural match contains
                if (product.getProductName().toLowerCase().contains(query)) {
                    matches.add(product);
                }
            }
        }

        return matches; // Return whatever is resident in memory
    }
    /**
     * Master put method with explicit control over fine-grained dirty states.
     */
    public synchronized void put(Product product, boolean productDirty, boolean stockDirty, boolean barcodeDirty) {
        if (product == null) return;

        this.cacheVersionCounter++;

        ProductCacheLine line = new ProductCacheLine(
                product,
                productDirty,
                stockDirty,
                barcodeDirty,
                this.cacheVersionCounter
        );

        this.cacheMap.put(product.getProductId(), line);

        if (product.getProductName() != null) {
            this.nameToIdIndex.put(product.getProductName().trim().toLowerCase(), product.getProductId());
        }

        if (product.getBarcodes() != null) {
            for (String barcode : product.getBarcodes()) {
                if (barcode != null) {
                    this.barcodeToIdIndex.put(barcode, product.getProductId());
                }
            }
        }

    }

    /**
     * Overloaded convenience method treating raw puts as full mutations.
     */
    public synchronized void put(Product product) {
        this.put(product, true, true, true);
    }

    /**
     * Flushes dirty data slices contextually out to disk and resets dirty tracks.
     */
    private void flushToDisk(ProductCacheLine line) {
        if (line == null || line.getProduct() == null) return;
        Product prod = line.getProduct();
        int id = prod.getProductId();

        // 1. Core main product properties mutations
        if (line.getDirtyBitStatus(ProductCacheLine.PRODUCT_BIT)) {
            productRepository.save(prod);
        }

        // 2. Granular inventory metric alterations
        if (line.getDirtyBitStatus(ProductCacheLine.STOCK_BIT)) {
           // stockRepository.upd(id, prod.getCurrentStock());
        }

        // 3. Independent index string arrays update tracking
        if (line.getDirtyBitStatus(ProductCacheLine.BARCODE_BIT)) {
            // productRepository.updateProductBarcodes(id, prod.getBarcodes());
        }

        // Structural Fix: Reset dirty bits globally ONLY AFTER selective flushes execute safely
        line.setDirtyBits(false);
    }

    /**
     * Global application checkpoint sweep.
     */
    public synchronized void flushAll() {
        System.out.println("Executing system checkpoint: Sweeping all dirty cache lines...");
        for (ProductCacheLine line : cacheMap.values()) {
            if (line.isDirtyBit()) {
                flushToDisk(line);
            }
        }
        System.out.println("System checkpoint completed successfully.");
    }

    /**
     * Evicts data explicitly out of tracking scope.
     */
    public synchronized void invalidate(int productId) {
        ProductCacheLine line = cacheMap.remove(productId);
        if (line == null) return;

        if (line.isDirtyBit()) {
            System.out.println("Invalidation Safeguard: Flushing dirty line to disk before removal.");
            flushToDisk(line);
        }

        Product p = line.getProduct();
        if (p.getProductName() != null) {
            this.nameToIdIndex.remove(p.getProductName());
        }

        if (p.getBarcodes() != null) {
            for (String barcode : p.getBarcodes()) {
                if (barcode != null) {
                    this.barcodeToIdIndex.remove(barcode);
                }
            }
        }
        System.out.println("Successfully evicted product ID " + productId + " from all cache layers.");
    }

    public long getCacheVersionCounter() {
        return cacheVersionCounter;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }
}
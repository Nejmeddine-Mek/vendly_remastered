package org.example.vendly_remastered.cache;

import org.example.vendly_remastered.cache.model.ProductCacheLine;
import org.example.vendly_remastered.models.entity.Product;
import org.example.vendly_remastered.repository.IProductRepository;
import org.example.vendly_remastered.repository.IStockRepository;

import java.math.BigDecimal;
import java.util.*;

public class ProductsCache {

    // Single thread-safe global instance
    private static volatile ProductsCache instance;

    // Monotonically increasing version counter (LSN tracking)
    private long cacheVersionCounter;

    // Strict threshold capacity for LRU bound
    private final int maxCacheSize;

    // Core database gateways
    private final IProductRepository productRepository;
    private final IStockRepository stockRepository;

    // Access-Order LinkedHashMap for LRU eviction tracking
    private final LinkedHashMap<Integer, ProductCacheLine> cacheMap;
    private final HashMap<String, Integer> barcodeToIdIndex = new HashMap<>();
    private final HashMap<String, Integer> nameToIdIndex = new HashMap<>();

    /**
     * Private Constructor: Configures LRU LinkedHashMap with pure memory eviction.
     */
    private ProductsCache(int capacity, IProductRepository repo, IStockRepository stockRepository) {
        this.maxCacheSize = capacity;
        this.productRepository = repo;
        this.stockRepository = stockRepository;
        this.cacheVersionCounter = 0;

        // Instantiate LinkedHashMap configured for access-order LRU tracking
        this.cacheMap = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, ProductCacheLine> eldest) {
                if (size() > maxCacheSize) {
                    ProductCacheLine eldestLine = eldest.getValue();
                    Product product = eldestLine.getProduct();

                    // WRITE-THROUGH GUARANTEE: The disk is already up to date!
                    // We only scrub pointer references to eliminate memory leaks upon LRU eviction.
                    if (product != null) {
                        if (product.getProductName() != null) {
                            nameToIdIndex.remove(product.getProductName().trim().toLowerCase());
                        }
                        if (product.getBarcodes() != null) {
                            for (String barcode : product.getBarcodes()) {
                                if (barcode != null) {
                                    barcodeToIdIndex.remove(barcode);
                                }
                            }
                        }
                    }
                    return true; // Authorize LRU structural eviction
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
     * Updates stock for multiple products in a batch, returns items that fell below minimum stock.
     *
     * @param stockUpdates Map of Product ID -> New Stock Quantity
     * @return Map of Product ID -> Current Stock level for products that triggered a low stock alert
     */
    public synchronized Map<Integer, BigDecimal> updateStockAndCheckAlerts(Map<Integer, BigDecimal> stockUpdates) {


        Map<Integer, BigDecimal> updatedBalances = stockRepository.updateStockBatch(stockUpdates);

        // Pass 2: Sync in-memory cache models with exact SQLite balances
        for (Map.Entry<Integer, BigDecimal> entry : updatedBalances.entrySet()) {
            int productId = entry.getKey();
            BigDecimal newBalance = entry.getValue();

            ProductCacheLine line = getLine(productId);
            if (line != null && line.getProduct() != null) {
                line.getProduct().setCurrentQuantity(newBalance);
                line.setDirtyBit(false, ProductCacheLine.STOCK_BIT); // Fresh on disk
            }
        }

        return updatedBalances; // UI checks if map !isEmpty() and pops alerts
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

        // Cache miss allocation (Read path: do not trigger write-through to disk)
        allocateReadHit(product);
        return product;
    }

    public synchronized ProductCacheLine getLine(int productId) {
        return cacheMap.get(productId);
    }

    /**
     * Secondary index lookups by Barcode string pattern.
     */
    public synchronized Product getByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return null;

        Integer productId = barcodeToIdIndex.get(barcode);
        if (productId != null) {
            return get(productId); // Promotes entry in LRU list via get()
        }

        System.out.println("Cache MISS for Barcode: " + barcode + " -> Querying DB.");
        Optional<Product> productOpt = productRepository.findByBarCode(barcode);
        if (productOpt.isPresent()) {
            Product p = productOpt.get();
            allocateReadHit(p);
            return p;
        }

        return null;
    }

    /**
     * In-memory or database lookup matching exact name string.
     */
    public synchronized Product getByName(String name) {
        if (name == null || name.isBlank()) return null;

        String key = name.trim().toLowerCase();
        Integer productId = nameToIdIndex.get(key);

        if (productId != null) {
            ProductCacheLine line = cacheMap.get(productId);
            if (line != null) {
                return line.getProduct();
            }
        }

        List<Product> res = productRepository.findByName(name);
        if (!res.isEmpty()) {
            Product p = res.get(0);
            allocateReadHit(p);
            return p;
        }

        return null;
    }

    /**
     * Sweeps internal memory structures for partial name query criteria matching.
     */
    public synchronized List<Product> searchByName(String name) {
        List<Product> matches = new ArrayList<>();
        if (name == null || name.isBlank()) return matches;

        String query = name.trim().toLowerCase();
        for (Map.Entry<String, Integer> entry : nameToIdIndex.entrySet()) {
            if (entry.getKey().contains(query)) {
                Product p = get(entry.getValue());
                if (p != null) {
                    matches.add(p);
                }
            }
        }

        if (!matches.isEmpty()) {
            return matches;
        }

        System.out.println("Cache MISS for Search Name: " + name + " -> Querying DB.");
        List<Product> prodOpt = productRepository.findByName(name);
        if (!prodOpt.isEmpty()) {
            for (Product p : prodOpt) {
                allocateReadHit(p);
                matches.add(p);
            }
        }

        return matches;
    }

    /**
     * Master WRITE-THROUGH Put: Persists immediately to storage, then updates in-memory indexes.
     */
    public synchronized void put(Product product) {
        if (product == null )
            return;
        ProductCacheLine line = allocateReadHit(product);

        persistSelectiveToDisk(line);
    }
    /**
     * Overloaded Write-Through Put: Unwraps a ProductCacheLine passed from UI/Modal layers
     * and immediately persists its underlying Product to storage and memory.
     */
    public synchronized void put(ProductCacheLine line) {
        if (line == null || line.getProduct() == null) return;

        Product prod = line.getProduct();

        // ORDER FIX: Check if it's a new product FIRST
        if (prod.getProductId() == 0) {
            // Step 1: Insert core product and get auto-generated primary key
            productRepository.save(prod); // Assigns prod.setProductId(newId) inside repo

            int newId = prod.getProductId();

            // Step 2: Now that we HAVE newId (> 0), insert initial stock / barcodes
            if (line.getDirtyBitStatus(ProductCacheLine.STOCK_BIT) && prod.getStock() != null) {
                stockRepository.saveStock(newId, prod.getStock());
            }
     /*       if (line.getDirtyBitStatus(ProductCacheLine.BARCODE_BIT) && prod.getBarcodes() != null) {
                productRepository.updateProductBarcodes(newId, prod.getBarcodes());
            }*/

            // Reset all flags since this new entity is now 100% persisted
            line.setAllDirtyBits(false);
        } else {
            // Existing Product: Execute selective write-through updates
            persistSelectiveToDisk(line);
        }

        // Step 3: Cache in memory using the guaranteed valid prod.getProductId() (> 0)
        allocateReadHit(prod);
    }
    /**
     * Helper to allocate unmutated records retrieved from DB without re-writing them to disk.
     */
    private ProductCacheLine allocateReadHit(Product product) {
        if (product == null) return null;

        this.cacheVersionCounter++;
        ProductCacheLine line = new ProductCacheLine(product, this.cacheVersionCounter);

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

        return line;
    }

    /**
     * Write-Through execution: Directly persists product changes to SQLite storage.
     */
    private void persistSelectiveToDisk(ProductCacheLine line) {
        Product prod = line.getProduct();


        // Core product properties updated (Name, Description, Base Price, etc.)
        if (line.getDirtyBitStatus(ProductCacheLine.PRODUCT_BIT)) {
            System.out.println("Product update/insert");
            productRepository.save(prod);
        }
        int id = prod.getProductId();
        // Stock quantities updated in dedicated stock table
        if (line.getDirtyBitStatus(ProductCacheLine.STOCK_BIT)) {
            System.out.println("stock update/insert");
            stockRepository.saveStock(id, prod.getCurrentStock());
        }
/*
        // Barcodes updated in child table
        if (line.getDirtyBitStatus(ProductCacheLine.BARCODE_BIT)) {
            productRepository.updateProductBarcodes(id, prod.getBarcodes());
        }

        // Promotional rules / discounts updated
        if (line.getDirtyBitStatus(ProductCacheLine.PROMO_BIT)) {
            productRepository.updatePromotions(id, prod.getPromotions());
        }
*/
        // Reset dirty flags immediately after disk sync succeeds
        line.setAllDirtyBits(false);
    }
    /**
     * Evicts data explicitly out of cache tracking.
     */
    public synchronized void invalidate(int productId) {
        ProductCacheLine line = cacheMap.remove(productId);
        if (line == null) return;

        Product p = line.getProduct();
        if (p != null) {
            if (p.getProductName() != null) {
                this.nameToIdIndex.remove(p.getProductName().trim().toLowerCase());
            }

            if (p.getBarcodes() != null) {
                for (String barcode : p.getBarcodes()) {
                    if (barcode != null) {
                        this.barcodeToIdIndex.remove(barcode);
                    }
                }
            }
        }
        System.out.println("Evicted product ID " + productId + " from cache memory.");
    }

    /**
     * Retained for compatibility—acts as a no-op checkpoint since writes are immediate.
     */
    public synchronized void flushAll() {
        System.out.println("Write-Through Active: All cached entries are already synchronized with disk.");
    }

    public long getCacheVersionCounter() {
        return cacheVersionCounter;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }
}
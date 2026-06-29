package org.example.vendly_remastered.cache.model;

import org.example.vendly_remastered.models.entity.Product;

public class ProductCacheLine {
    private boolean dirtyBit;
    private long cacheVersion; // Our sequential modification tracker (like an LSN)
    private Product product;

    public ProductCacheLine(Product prod, long currentVersion) {
        this.product = prod;
        this.dirtyBit = false;
        this.cacheVersion = currentVersion;
    }

    public ProductCacheLine(Product prod, boolean changeBit, long currentVersion) {
        this.product = prod;
        this.dirtyBit = changeBit;
        this.cacheVersion = currentVersion;
    }

    public boolean isDirtyBit() {
        return dirtyBit;
    }

    public void setDirtyBit(boolean dirtyBit) {
        this.dirtyBit = dirtyBit;
    }

    public long getCacheVersion() {
        return cacheVersion;
    }

    public void setCacheVersion(long cacheVersion) {
        this.cacheVersion = cacheVersion;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
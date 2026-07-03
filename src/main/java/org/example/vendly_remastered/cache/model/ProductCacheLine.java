package org.example.vendly_remastered.cache.model;

import org.example.vendly_remastered.models.entity.Product;

import java.util.Arrays;

public class ProductCacheLine {
    public static final int PRODUCT_BIT = 0;
    public static final int STOCK_BIT = 1;
    public static final int BARCODE_BIT = 2;

    private boolean dirtyBits[];
    // product table, stock table, barcode table, we should have at least 3 dirty bits
    private long cacheVersion; // Our sequential modification tracker (like an LSN)
    private Product product;

    // 1. Explicit Control: Pass exact states for all tracking targets
    public ProductCacheLine(Product prod, boolean productDirty, boolean stockDirty, boolean barcodeDirty, long currentVersion) {
        this.product = prod;
        this.cacheVersion = currentVersion;
        this.dirtyBits = new boolean[]{productDirty, stockDirty, barcodeDirty};
    }

    // 2. Clean Read Overload: Pure DB fetches start completely clean (all false)
    public ProductCacheLine(Product prod, long currentVersion) {
        this(prod, false, false, false, currentVersion);
    }

    public boolean isDirtyBit() {
        for(boolean bit:  dirtyBits){
            if (bit) return true;
        }
        return false;
    }
    public void setDirtyBit(boolean dirtyBit,int index) {
        if (index >= 0 && index < this.dirtyBits.length) {
            this.dirtyBits[index] = dirtyBit;
        }
    }

    public void setDirtyBits(boolean val) {
        for(boolean bit: dirtyBits){
            bit = val;
        }
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

    public boolean getDirtyBitStatus(int productBit) {
        return dirtyBits[productBit];
    }
}
package org.example.vendly_remastered.cache.model;

import org.example.vendly_remastered.models.entity.Product;

import java.util.Arrays;

public class ProductCacheLine {
    public static final int PRODUCT_BIT = 0;
    public static final int STOCK_BIT = 1;
    public static final int BARCODE_BIT = 2;
    public static final int PROMO_BIT = 3;

    private boolean[] dirtyBits;
    private long cacheVersion; // Sequential modification tracker (like an LSN)
    private Product product;

    // 1. Explicit Control: Pass exact states for all tracking targets (4 bits now)
    public ProductCacheLine(Product prod, boolean productDirty, boolean stockDirty, boolean barcodeDirty, boolean promoDirty, long currentVersion) {
        this.product = prod;
        this.cacheVersion = currentVersion;
        // Size 4 array to accommodate PROMO_BIT (index 3)
        this.dirtyBits = new boolean[]{productDirty, stockDirty, barcodeDirty, promoDirty};
    }

    // 2. Clean Read Overload: Pure DB fetches start completely clean (all false)
    public ProductCacheLine(Product prod, long currentVersion) {
        this(prod, false, false, false, false, currentVersion);
    }

    public boolean isDirtyBit() {
        for (boolean bit : dirtyBits) {
            if (bit) return true;
        }
        return false;
    }

    public void setDirtyBit(boolean dirtyBit, int index) {
        if (index >= 0 && index < this.dirtyBits.length) {
            this.dirtyBits[index] = dirtyBit;
        }
    }

    // Fixed: Mutates actual array indices rather than for-each local copies
    public void setAllDirtyBits(boolean val) {
        for (int i = 0; i < dirtyBits.length; i++) {
            dirtyBits[i] = val;
        }
    }

    public boolean getDirtyBitStatus(int bitIndex) {
        if (bitIndex >= 0 && bitIndex < dirtyBits.length) {
            return dirtyBits[bitIndex];
        }
        return false;
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
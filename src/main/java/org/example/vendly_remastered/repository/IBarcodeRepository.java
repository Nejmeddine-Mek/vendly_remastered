package org.example.vendly_remastered.repository;

import java.util.List;
import java.util.Map;

public interface IBarcodeRepository {
    public void insertBarcode(Map<String, Integer> barcodeIdMap);
    public void deleteBarcode(List<String> barcodes);
}

package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.models.entity.SaleHeader;

import java.time.LocalDate;
import java.util.List;

public interface IPurchaseHeaderRepository {
    public int savePurchaseHeader(PurchaseHeader purchaseHeader);
    public List<PurchaseHeader> findAll();
    public List<PurchaseHeader> findByDate(LocalDate date);
    public List<PurchaseHeader> getUnpaidPurchases(int supplierId);
    public void updateHeaders(List<PurchaseHeader> purchaseHeaders);
    public List<PurchaseHeader> getByRange(LocalDate beginning, LocalDate end);
}

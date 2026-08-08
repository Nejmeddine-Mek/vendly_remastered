package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.PurchaseDetailItem;

import java.util.List;

public interface IPurchaseDetailsRepository {

    public List<PurchaseDetailItem> getPurchaseItems(int purchaseId);
    public void savePurchaseItems(List<PurchaseDetailItem> items);
}

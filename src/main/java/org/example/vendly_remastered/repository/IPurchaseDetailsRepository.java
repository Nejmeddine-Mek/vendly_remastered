package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.PurchaseDetailItem;

import java.util.List;

public interface IPurchaseDetailsRepository {
    List<PurchaseDetailItem> getPurchaseDetails(Long purchaseId);
}

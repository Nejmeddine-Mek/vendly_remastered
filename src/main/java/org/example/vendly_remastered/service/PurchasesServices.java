package org.example.vendly_remastered.service;

import org.example.vendly_remastered.models.entity.PurchaseHeader;
import org.example.vendly_remastered.repository.IPurchaseHeaderRepository;
import org.example.vendly_remastered.repository.impl.PurchaseHeaderSQLiteImpl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class PurchasesServices {
    IPurchaseHeaderRepository purchaseHeaderRepository;

    public PurchasesServices(Connection connection){
        purchaseHeaderRepository = new PurchaseHeaderSQLiteImpl(connection);
    }
    public PurchasesServices(IPurchaseHeaderRepository repo){ purchaseHeaderRepository = repo; }
    public void resolveDebts(BigDecimal amount, int supplierId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return;

        List<PurchaseHeader> unpaidSales = purchaseHeaderRepository.getUnpaidPurchases(supplierId);
        if (unpaidSales == null || unpaidSales.isEmpty())
            return; // Return the full amount as surplus/unallocated credit

        List<PurchaseHeader> updatedHeaders = new ArrayList<>();

        for (PurchaseHeader header : unpaidSales) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                break; // No amount left to distribute
            }

            BigDecimal remaining = header.getPurchaseCost().subtract(header.getAmountPaid());

            if (amount.compareTo(remaining) >= 0) {
                // Fully pay off this purchase invoice
                header.setAmountPaid(header.getPurchaseCost());
                amount = amount.subtract(remaining);
                updatedHeaders.add(header);
            } else {
                // Partially pay off this purchase invoice and exhaust the available amount
                header.setAmountPaid(header.getAmountPaid().add(amount));
                amount = BigDecimal.ZERO;
                updatedHeaders.add(header);
                break;
            }
        }

        // Persist all modified purchase headers in batch
        if (!updatedHeaders.isEmpty()) {
            purchaseHeaderRepository.updateHeaders(updatedHeaders);
        }

        // Returns leftover amount if payment exceeded total outstanding debt (supplier credit)
    }
}

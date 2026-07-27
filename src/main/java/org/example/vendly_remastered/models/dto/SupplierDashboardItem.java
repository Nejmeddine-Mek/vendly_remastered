package org.example.vendly_remastered.models.dto;

public record SupplierDashboardItem(
        long supplierId,
        String supplierName,
        int purchasesCount,
        double totalSpent
) {}

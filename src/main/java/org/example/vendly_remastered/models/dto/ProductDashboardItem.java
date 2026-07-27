package org.example.vendly_remastered.models.dto;

public record ProductDashboardItem(
        long productId,
        String productName,
        int unitsSold,
        double totalRevenue
) {}

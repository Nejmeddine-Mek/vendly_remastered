package org.example.vendly_remastered.models.dto;

public record ClientDashboardItem(
        long clientId,
        String clientName,
        int ordersCount,
        double totalSpent
) {}

package org.example.vendly_remastered.models.dto;

import java.time.LocalDate;

public record RevenueDashboardItem(
        LocalDate date,
        double totalRevenue,
        double totalPurchases
) {
    // Helper to format dates nicely for Chart X-Axis (e.g. "2026-07-27")
    public String getDateLabel() {
        return date != null ? date.toString() : "";
    }
}

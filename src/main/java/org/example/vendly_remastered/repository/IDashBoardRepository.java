package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.dto.ClientDashboardItem;
import org.example.vendly_remastered.models.dto.SupplierDashboardItem;

import java.time.LocalDate;
import java.util.List;

public interface IDashBoardRepository {
    public List<ClientDashboardItem> getTopClients();
    public List<ClientDashboardItem> getTopClients(LocalDate beginningDate, LocalDate endDate);

    public List<SupplierDashboardItem> getTopSuppliers();
    public List<SupplierDashboardItem> getTopSuppliers(LocalDate beginningDate, LocalDate endDate);
}

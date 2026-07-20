package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.SaleHeader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ISalesHeaderRepository {
    public int saveSaleHeader(SaleHeader sale);
    public List<SaleHeader> findAll();
    public List<SaleHeader> findByDate(LocalDate date);
    public List<SaleHeader> getUnpaidSales(int clientId);
    public void updateHeaders(List<SaleHeader> saleHeaders);
    public List<SaleHeader> getByRange(LocalDate beginning, LocalDate end);
}

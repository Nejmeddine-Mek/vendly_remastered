package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.SaleHeader;

import java.util.List;

public interface ISalesHeaderRepository {
    public boolean saveSaleHeader(SaleHeader sale);
    public List<SaleHeader> findAll();
}

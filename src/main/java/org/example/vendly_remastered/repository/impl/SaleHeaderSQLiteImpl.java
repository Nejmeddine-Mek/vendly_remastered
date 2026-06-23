package org.example.vendly_remastered.repository.impl;

import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;

import java.sql.Connection;
import java.util.List;

public class SaleHeaderSQLiteImpl implements ISalesHeaderRepository {
    private Connection connection;
    public SaleHeaderSQLiteImpl(Connection conn){ this.connection = conn; }

    @Override
    public boolean saveSaleHeader(SaleHeader sale) {
        return false;
    }

    @Override
    public List<SaleHeader> findAll() {
        return List.of();
    }
}

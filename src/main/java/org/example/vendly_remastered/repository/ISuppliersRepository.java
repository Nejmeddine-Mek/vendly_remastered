package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.Supplier;

import java.util.List;

public interface ISuppliersRepository {
    public List<Supplier> findAll();
    public List<Supplier> findByName(String name);
    public int AddSupplier(Supplier supplier);
    public void updateSupplier(Supplier supplier);
}

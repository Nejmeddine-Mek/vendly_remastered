package org.example.vendly_remastered.repository;

import org.example.vendly_remastered.models.entity.SaleDetailItem;

import java.util.List;

public interface ISaleDetailsRepository {
    public void saveSaleDetails(List<SaleDetailItem> soldItems);

}

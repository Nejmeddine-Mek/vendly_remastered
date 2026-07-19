package org.example.vendly_remastered.service;

import org.example.vendly_remastered.models.entity.SaleHeader;
import org.example.vendly_remastered.repository.ISalesHeaderRepository;
import org.example.vendly_remastered.repository.impl.SaleHeaderSQLiteImpl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class SalesServices {
    ISalesHeaderRepository salesHeaderRepository;

    public SalesServices(Connection connection){
        salesHeaderRepository = new SaleHeaderSQLiteImpl(connection);
    }
    public SalesServices(ISalesHeaderRepository repo){ salesHeaderRepository = repo; }
    public BigDecimal resolveDebts(BigDecimal amount, int clientId){
        if(amount == null)
            return BigDecimal.ZERO;

        List<SaleHeader> unpaidSales = salesHeaderRepository.getUnpaidSales(clientId);
        if(unpaidSales.isEmpty())
            return amount;
        List<SaleHeader>  updatedHeaders = new ArrayList<>();
        for(SaleHeader header : unpaidSales){
            BigDecimal remaining = header.getSaleTotal().subtract(header.getPaidAmount());
            if(amount.compareTo(remaining) >= 0){
                // in this case, add the amount to cap the sale, subtract it from the amount, and move to the next one
                header.setPaidAmount(header.getSaleTotal());
                amount = amount.subtract(remaining);
                // TODO: add the header into a list of header that we will need to reinsert later
                updatedHeaders.add(header);
            } else if(amount.compareTo(remaining) < 0){
                // put all the remaining in there, and set amount to 0, then break
                //
                header.setPaidAmount(header.getPaidAmount().add(amount));
                amount =BigDecimal.ZERO;
                // TODO: add to the list to reinsert
                updatedHeaders.add(header);
                break;
            }
        }
        salesHeaderRepository.updateHeaders(updatedHeaders);
        return amount;
    }
}

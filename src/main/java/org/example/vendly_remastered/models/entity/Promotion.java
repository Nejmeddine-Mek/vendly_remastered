package org.example.vendly_remastered.models.entity;

import java.math.BigDecimal;

public class Promotion {
    BigDecimal quantity;
    BigDecimal price;
    public Promotion(){}
    public Promotion(BigDecimal quantity,BigDecimal price ){
        this.quantity = quantity;
        this.price = price;
    }
    public void setPrice(BigDecimal newPrice){ this.price = newPrice; }
    public BigDecimal getPrice(){ return price; }

    public void setQuantity(BigDecimal newQuantity){ this.quantity = newQuantity; }
    public BigDecimal getQuantity(){ return quantity; }
}

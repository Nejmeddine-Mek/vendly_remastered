package org.example.vendly_remastered.models.dto;

import javafx.beans.property.*;

import java.math.BigDecimal;

public class CartItem {
    private IntegerProperty productId;
    private StringProperty barCode;
    private StringProperty productName;
    private ObjectProperty<BigDecimal> quantity;
    private ObjectProperty<BigDecimal> unitPrice;
    private ObjectProperty<BigDecimal> subTotal;

    public CartItem(int productId,String barCode, String productName, BigDecimal unitPrice, BigDecimal quantity) {
        this.productId = new SimpleIntegerProperty(productId);
        this.barCode = new SimpleStringProperty(barCode);
        this.productName = new SimpleStringProperty(productName);
        this.unitPrice = new SimpleObjectProperty<>(unitPrice);
        this.quantity = new SimpleObjectProperty<>(quantity);
        // Automatically compute subtotal: price * quantity
        this.subTotal = new SimpleObjectProperty<>(unitPrice.multiply(quantity));
    }
    public IntegerProperty productIdProperty() { return productId; }
    public StringProperty barCodeProperty() { return barCode; }
    public StringProperty productNameProperty() { return productName; }
    public ObjectProperty<BigDecimal> unitPriceProperty() { return unitPrice; }
    public ObjectProperty<BigDecimal> quantityProperty() { return quantity; }
    public ObjectProperty<BigDecimal> subtotalProperty() { return subTotal; }

    // --- Standard Getters & Setters ---
    public int getProductId() { return productId.get(); }
    public String getProductName() { return productName.get(); }
    public BigDecimal getUnitPrice() { return unitPrice.get(); }
    public BigDecimal getQuantity() { return quantity.get(); }
    public BigDecimal getSubtotal() { return subTotal.get(); }
    public String getBarCode() { return barCode.get(); }
    /**
     * Updates the quantity and automatically recalculates the subtotal defensively
     */
    public void setQuantity(BigDecimal quantity) {
        this.quantity.set(quantity);
        this.subTotal.set(this.getUnitPrice().multiply(quantity));
    }




}

package org.example.vendly_remastered.models.entity;

public class Category {
    private short categoryId;
    private String categoryName;

    public Category(){}
    public Category(short id, String name){
        categoryId = id;
        categoryName = name;
    }


    public short getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(short categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
    @Override
    public String toString() {
        return categoryName;
    }
}

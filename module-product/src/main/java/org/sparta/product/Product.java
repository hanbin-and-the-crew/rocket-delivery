package org.sparta.product;

public class Product {
    private Long id;
    private String name;
    private Integer price;
    private Integer stock;

    public Product() {
    }

    public Product(Long id, String name, Integer price, Integer stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    public boolean isAvailable() {
        return this.stock > 0;
    }
}
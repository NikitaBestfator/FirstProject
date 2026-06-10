package org.example;

public class Product {
    private int id;
    private String name;
    private double price;
    private int count;
    private int discount;
    private String photo;
    private String categoryName;
    private String brandName;
    private String supplierName;
    private String description;
    private String unit;

    // Конструктор с полной информацией (все поля)
    public Product(int id, String name, double price, int count, int discount, String photo,
                   String categoryName, String brandName, String supplierName,
                   String description, String unit) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.count = count;
        this.discount = discount;
        this.photo = photo;
        this.categoryName = categoryName;
        this.brandName = brandName;
        this.supplierName = supplierName;
        this.description = description;
        this.unit = unit;
    }

    // Геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getCount() { return count; }
    public int getDiscount() { return discount; }
    public String getPhoto() { return photo; }
    public String getCategoryName() { return categoryName; }
    public String getBrandName() { return brandName; }
    public String getSupplierName() { return supplierName; }
    public String getDescription() { return description; }
    public String getUnit() { return unit; }
}
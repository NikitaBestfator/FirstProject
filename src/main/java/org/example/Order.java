package org.example;

import java.time.LocalDateTime;

public class Order {
    private int id;
    private int userId;
    private String userName;
    private double totalCost;
    private LocalDateTime orderDate;
    private String status;

    public Order(int id, int userId, String userName, double totalCost, LocalDateTime orderDate, String status) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.totalCost = totalCost;
        this.orderDate = orderDate;
        this.status = status;
    }

    // Геттеры (обязательны для TableView)
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public double getTotalCost() { return totalCost; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public String getStatus() { return status; }
}

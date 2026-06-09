package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        // Данные для подключения
        String url = "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC";
        String user = "admin";      // мой пользователь MySQL
        String password = "admin123";  // мой пароль MySQL

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ Подключение к базе данных успешно!");
            System.out.println("Схема: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("❌ Ошибка подключения!");
            System.err.println("Код: " + e.getErrorCode());
            System.err.println("Сообщение: " + e.getMessage());
        }
    }
}
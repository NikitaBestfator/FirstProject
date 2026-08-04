package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDateTime;

public class OrderForm {

    private Order editingOrder;
    private Stage stage;
    private Runnable onSaveCallback;

    // Поля формы
    private ComboBox<String> userCombo;
    private TextField totalCostField;
    private ComboBox<String> statusCombo;
    private TableView<CartItem> cartTable;
    private ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private ComboBox<String> productCombo;
    private TextField quantityField;

    public OrderForm(Order order, Runnable onSaveCallback) {
        this.editingOrder = order;
        this.onSaveCallback = onSaveCallback;
        showWindow();
    }

    private void showWindow() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(editingOrder == null ? "Добавление заказа" : "Редактирование заказа");

        // Установка иконки
//        try {
//            javafx.scene.image.Image icon = new javafx.scene.image.Image(getClass().getResourceAsStream("/images/icon.png"));
//            stage.getIcons().add(icon);
//        } catch (Exception e) {
//            System.err.println("Иконка не загружена: " + e.getMessage());
//        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);

        int row = 0;

        // Пользователь
        grid.add(new Label("Пользователь:"), 0, row);
        userCombo = new ComboBox<>();
        loadUsers();
        grid.add(userCombo, 1, row++);

        // Статус
        grid.add(new Label("Статус:"), 0, row);
        statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("NEW", "PAID", "CANCELED");
        statusCombo.setValue("NEW");
        grid.add(statusCombo, 1, row++);

        // Корзина (таблица товаров)
        grid.add(new Label("Товары в заказе:"), 0, row);
        cartTable = new TableView<>();
        cartTable.setPrefHeight(200);
        cartTable.setPrefWidth(400);

        TableColumn<CartItem, String> productCol = new TableColumn<>("Товар");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

        TableColumn<CartItem, Integer> countCol = new TableColumn<>("Количество");
        countCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<CartItem, Double> priceCol = new TableColumn<>("Цена");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("priceAtMoment"));

        TableColumn<CartItem, Double> totalCol = new TableColumn<>("Сумма");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        cartTable.getColumns().addAll(productCol, countCol, priceCol, totalCol);
        cartTable.setItems(cartItems);

        VBox cartBox = new VBox(5, cartTable);
        grid.add(cartBox, 1, row++);

        // Добавление товара в корзину
        grid.add(new Label("Добавить товар:"), 0, row);
        HBox addProductBox = new HBox(5);
        productCombo = new ComboBox<>();
        loadProducts();
        productCombo.setPrefWidth(200);
        quantityField = new TextField();
        quantityField.setPromptText("Кол-во");
        quantityField.setPrefWidth(80);
        Button addBtn = new Button("+");
        addBtn.setOnAction(e -> addToCart());

        addProductBox.getChildren().addAll(productCombo, quantityField, addBtn);
        grid.add(addProductBox, 1, row++);

        // Кнопка удаления из корзины
        Button removeBtn = new Button("Удалить выбранный товар из заказа");
        removeBtn.setOnAction(e -> {
            CartItem selected = cartTable.getSelectionModel().getSelectedItem();
            if (selected != null) cartItems.remove(selected);
        });
        grid.add(removeBtn, 1, row++);

        // Общая сумма
        Label totalLabel = new Label("Общая сумма: 0.00 руб.");
        totalCostField = new TextField();
        totalCostField.setEditable(false);
        totalCostField.setPrefWidth(150);
        HBox totalBox = new HBox(10, totalLabel, totalCostField);
        grid.add(totalBox, 1, row++);

        // Кнопки
        Button saveBtn = new Button("Сохранить");
        saveBtn.setOnAction(e -> saveOrder());
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        grid.add(buttons, 1, row);

        // Заполняем данными при редактировании
        if (editingOrder != null) {
            fillFields();
        }

        // Обновление общей суммы при изменении корзины
        cartItems.addListener((javafx.collections.ListChangeListener<CartItem>) c -> updateTotal());

        Scene scene = new Scene(grid, 700, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void loadUsers() {
        String sql = "SELECT id, name FROM users WHERE role != 'GUEST'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                userCombo.getItems().add(rs.getInt("id") + " - " + rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadProducts() {
        String sql = "SELECT id, name, price FROM products WHERE count > 0";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                productCombo.getItems().add(rs.getInt("id") + " - " + rs.getString("name") + " (" + rs.getDouble("price") + " руб.)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addToCart() {
        if (productCombo.getValue() == null || quantityField.getText().isEmpty()) {
            showAlert("Ошибка", "Выберите товар и укажите количество");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText());
            if (quantity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Количество должно быть положительным числом");
            return;
        }

        String selected = productCombo.getValue();
        int productId = Integer.parseInt(selected.split(" - ")[0]);
        String productName = selected.split(" - ")[1].split(" \\(")[0];
        double price = Double.parseDouble(selected.split("\\(")[1].replace(" руб.)", ""));

        try (Connection conn = getConnection()) {
            String checkSql = "SELECT count FROM products WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int available = rs.getInt("count");
                    if (quantity > available) {
                        showAlert("Ошибка", "Недостаточно товара на складе.\nДоступно: " + available + " шт.\nЗаказано: " + quantity);
                        return;
                    }
                } else {
                    showAlert("Ошибка", "Товар не найден");
                    return;
                }
            }
        } catch (SQLException e) {
            showAlert("Ошибка БД", "Не удалось проверить остаток: " + e.getMessage());
            return;
        }

        CartItem item = new CartItem(productId, productName, quantity, price);
        cartItems.add(item);
        productCombo.setValue(null);
        quantityField.clear();
    }

    private void updateTotal() {
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        totalCostField.setText(String.format("%.2f руб.", total));
    }

    private void fillFields() {
        // Загружаем пользователя
        String userName = editingOrder.getUserName();
        userCombo.setValue(editingOrder.getUserId() + " - " + userName);
        statusCombo.setValue(editingOrder.getStatus());

        // Загружаем товары заказа
        String sql = "SELECT po.product_id, p.name, po.product_count, po.price_at_moment " +
                "FROM products_orders po JOIN products p ON po.product_id = p.id WHERE po.order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, editingOrder.getId());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                cartItems.add(new CartItem(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getInt("product_count"),
                        rs.getDouble("price_at_moment")
                ));
            }
            updateTotal();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveOrder() {
        if (userCombo.getValue() == null) {
            showAlert("Ошибка", "Выберите пользователя");
            return;
        }
        if (cartItems.isEmpty()) {
            showAlert("Ошибка", "Добавьте хотя бы один товар в заказ");
            return;
        }

        int userId = Integer.parseInt(userCombo.getValue().split(" - ")[0]);
        String status = statusCombo.getValue();
        double total = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        try (Connection conn = getConnection()) {
            if (editingOrder == null) {
                // INSERT нового заказа
                String insertOrder = "INSERT INTO orders (user_id, total_cost, status, order_date) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, userId);
                    pstmt.setDouble(2, total);
                    pstmt.setString(3, status);
                    pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    pstmt.executeUpdate();

                    ResultSet rs = pstmt.getGeneratedKeys();
                    rs.next();
                    int orderId = rs.getInt(1);

                    // Добавляем позиции заказа
                    String insertItem = "INSERT INTO products_orders (order_id, product_id, product_count, price_at_moment) VALUES (?, ?, ?, ?)";
                    for (CartItem item : cartItems) {
                        try (PreparedStatement pstmt2 = conn.prepareStatement(insertItem)) {
                            pstmt2.setInt(1, orderId);
                            pstmt2.setInt(2, item.getProductId());
                            pstmt2.setInt(3, item.getQuantity());
                            pstmt2.setDouble(4, item.getPriceAtMoment());
                            pstmt2.executeUpdate();
                        }
                        // Обновляем остаток товара
                        String updateStock = "UPDATE products SET count = count - ? WHERE id = ?";
                        try (PreparedStatement pstmt3 = conn.prepareStatement(updateStock)) {
                            pstmt3.setInt(1, item.getQuantity());
                            pstmt3.setInt(2, item.getProductId());
                            pstmt3.executeUpdate();
                        }
                    }
                }
            } else {
                // UPDATE существующего заказа
                String updateOrder = "UPDATE orders SET user_id=?, total_cost=?, status=? WHERE id=?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateOrder)) {
                    pstmt.setInt(1, userId);
                    pstmt.setDouble(2, total);
                    pstmt.setString(3, status);
                    pstmt.setInt(4, editingOrder.getId());
                    pstmt.executeUpdate();
                }

                // Удаляем старые позиции и возвращаем остатки
                String deleteItems = "DELETE FROM products_orders WHERE order_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteItems)) {
                    pstmt.setInt(1, editingOrder.getId());
                    pstmt.executeUpdate();
                }

                // Добавляем новые позиции
                String insertItem = "INSERT INTO products_orders (order_id, product_id, product_count, price_at_moment) VALUES (?, ?, ?, ?)";
                for (CartItem item : cartItems) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertItem)) {
                        pstmt.setInt(1, editingOrder.getId());
                        pstmt.setInt(2, item.getProductId());
                        pstmt.setInt(3, item.getQuantity());
                        pstmt.setDouble(4, item.getPriceAtMoment());
                        pstmt.executeUpdate();
                    }
                }
            }

            showAlert("Успех", "Заказ сохранён");
            if (onSaveCallback != null) onSaveCallback.run();
            stage.close();
        } catch (SQLException e) {
            showAlert("Ошибка БД", e.getMessage());
        }
    }
    // Подключение к БД
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
    }
    // Создает информационное окошко
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Вспомогательный класс для позиции в корзине
    public static class CartItem {
        private int productId;
        private String productName;
        private int quantity;
        private double priceAtMoment;

        public CartItem(int productId, String productName, int quantity, double priceAtMoment) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.priceAtMoment = priceAtMoment;
        }

        public int getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPriceAtMoment() { return priceAtMoment; }
        public double getTotal() { return quantity * priceAtMoment; }
    }
}

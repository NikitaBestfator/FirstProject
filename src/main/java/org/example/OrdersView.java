package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.sql.*;
import java.time.LocalDateTime;

public class OrdersView extends Application {

    private User currentUser;
    private ObservableList<Order> orderList = FXCollections.observableArrayList();
    private TableView<Order> tableView = new TableView<>();
    private Stage primaryStage;

    public OrdersView(User user) {
        this.currentUser = user;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Управление заказами - " + currentUser.getName());

        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(450);
        // Создание иконки
//        try {
//            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
//            primaryStage.getIcons().add(icon);
//        } catch (Exception e) {
//            System.err.println("Иконка не загружена: " + e.getMessage());
//        }
        loadOrders();
        createOrderTable();

        VBox buttonBar = new VBox(10);
        buttonBar.setPadding(new Insets(10));

        // Кнопка "Удалить" только для ADMIN
        if (currentUser.getRole().equals("ADMIN")) {
            Button addButton = new Button("Добавить заказ");
            addButton.setOnAction(e -> openOrderForm(null));

            Button editButton = new Button("Редактировать заказ");
            editButton.setOnAction(e -> {
                Order selected = tableView.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showAlert("Внимание", "Сначала выберите заказ");
                } else {
                    openOrderForm(selected);
                }
            });

            Button deleteButton = new Button("Удалить заказ");
            deleteButton.setOnAction(e -> deleteSelectedOrder());

            buttonBar.getChildren().addAll(addButton, editButton, deleteButton);
        }

        // Кнопка "Назад" для всех
        Button backButton = new Button("← Назад");
        backButton.setOnAction(e -> {
            primaryStage.close();
            MainView mainView = new MainView(currentUser);
            Stage mainStage = new Stage();
            mainView.start(mainStage);
        });

        BorderPane layout = new BorderPane();
        layout.setTop(backButton);
        layout.setCenter(tableView);
        layout.setRight(buttonBar);

        Scene scene = new Scene(layout, 900, 500);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadOrders() {
        orderList.clear();
        String sql = "SELECT o.id, o.user_id, u.name AS user_name, o.total_cost, o.order_date, o.status " +
                "FROM orders o JOIN users u ON o.user_id = u.id ORDER BY o.order_date DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("user_name"),
                        rs.getDouble("total_cost"),
                        rs.getTimestamp("order_date").toLocalDateTime(),
                        rs.getString("status")
                );
                orderList.add(order);
            }
            tableView.setItems(orderList);
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить заказы: " + e.getMessage());
        }
    }

    private void createOrderTable() {
        tableView.getColumns().clear();

        TableColumn<Order, Integer> idCol = new TableColumn<>("ID заказа");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Order, String> userCol = new TableColumn<>("Пользователь");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userName"));

        TableColumn<Order, Double> totalCol = new TableColumn<>("Сумма");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalCost"));

        TableColumn<Order, LocalDateTime> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        TableColumn<Order, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.getColumns().addAll(idCol, userCol, totalCol, dateCol, statusCol);
        tableView.setItems(orderList);
    }

    private void deleteSelectedOrder() {
        Order selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Внимание", "Сначала выберите заказ");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Удалить заказ №" + selected.getId() + "?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (Connection conn = getConnection()) {
            // Сначала удаляем позиции заказа
            String deleteItems = "DELETE FROM products_orders WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteItems)) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
            }
            // Затем сам заказ
            String deleteOrder = "DELETE FROM orders WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrder)) {
                pstmt.setInt(1, selected.getId());
                pstmt.executeUpdate();
            }
            showAlert("Успех", "Заказ удалён");
            loadOrders();
        } catch (SQLException e) {
            showAlert("Ошибка БД", e.getMessage());
        }
    }
    // Открывает окно редактирования заказа и настраивает автоматическое обновление таблиц
    private void openOrderForm(Order order) {
        new OrderForm(order, () -> {
            loadOrders();
            tableView.refresh();
        });
    }
    // Подключение к БД
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
    }
    // Уведомления о каком-то событии действий пользователя
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
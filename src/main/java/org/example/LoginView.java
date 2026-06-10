package org.example;

import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.image.Image;

import java.sql.*;

public class LoginView extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Авторизация - Обувной магазин");

        // Установка иконки
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            // игнорируем — иконка не загрузилась, приложение работает
            System.err.println("Иконка не загружена: " + e.getMessage());
        }
        // Создаём элементы формы
        Label lblLogin = new Label("Логин:");
        TextField txtLogin = new TextField();
        Label lblPassword = new Label("Пароль:");
        PasswordField txtPassword = new PasswordField();
        Button btnLogin = new Button("Войти");
        Button btnGuest = new Button("Войти как гость");

        // Располагаем элементы
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.add(lblLogin, 0, 0);
        grid.add(txtLogin, 1, 0);
        grid.add(lblPassword, 0, 1);
        grid.add(txtPassword, 1, 1);
        grid.add(btnLogin, 0, 2);
        grid.add(btnGuest, 1, 2);

        // Действие для кнопки "Войти"
        btnLogin.setOnAction(e -> {
            String login = txtLogin.getText();
            String password = txtPassword.getText();
            User user = authenticate(login, password);
            if (user != null) {
                openMainWindow(user);
            } else {
                showAlert("Ошибка", "Неверный логин или пароль");
            }
        });

        // Действие для кнопки "Гость"
        btnGuest.setOnAction(e -> {
            User guest = new User(0, "guest", "", "Гость", "GUEST");
            openMainWindow(guest);
        });

        Scene scene = new Scene(grid, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Метод проверки логина/пароля в БД
    private User authenticate(String login, String password) {
        String sql = "SELECT id, login, password, name, role FROM users WHERE login = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("name"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Открыть главное окно после входа
    private void openMainWindow(User user) {
        MainView mainView = new MainView(user);
        Stage mainStage = new Stage();
        mainView.start(mainStage);
        primaryStage.close(); // закрываем окно входа
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

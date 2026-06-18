package org.example;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;

public class ProductForm {

    private Product editingProduct;
    private Stage stage;
    private Runnable onSaveCallback;

    // Поля формы
    private TextField nameField, priceField, countField, discountField, unitField;
    private TextArea descriptionArea;
    private ComboBox<String> categoryCombo, brandCombo, supplierCombo;
    private ImageView photoView;
    private String currentPhotoPath;

    public ProductForm(Product product, Runnable onSaveCallback) {
        this.editingProduct = product;
        this.onSaveCallback = onSaveCallback;
        showWindow();
    }
    // Создает и настраивает окно для добавления и редактирования товара
    private void showWindow() {
        stage = new Stage();

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(editingProduct == null ? "Добавление товара" : "Редактирование товара");

        stage.setMinWidth(550);
        stage.setMinHeight(600);

        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Иконка не загружена: " + e.getMessage());
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);

        int row = 0;

        // Фото
        Label photoLabel = new Label("Фото:");
        photoView = new ImageView();
        photoView.setFitWidth(150);
        photoView.setFitHeight(150);
        photoView.setPreserveRatio(true);
        Button uploadPhotoBtn = new Button("Загрузить фото");
        uploadPhotoBtn.setOnAction(e -> uploadPhoto());

        VBox photoBox = new VBox(5, photoView, uploadPhotoBtn);
        grid.add(photoLabel, 0, row);
        grid.add(photoBox, 1, row++);

        // Наименование
        grid.add(new Label("Наименование:"), 0, row);
        nameField = new TextField();
        nameField.setPrefWidth(400);
        grid.add(nameField, 1, row++);

        // Категория
        grid.add(new Label("Категория:"), 0, row);
        categoryCombo = new ComboBox<>();
        loadCategories();
        grid.add(categoryCombo, 1, row++);

        // Производитель
        grid.add(new Label("Производитель:"), 0, row);
        brandCombo = new ComboBox<>();
        loadBrands();
        grid.add(brandCombo, 1, row++);

        // Поставщик
        grid.add(new Label("Поставщик:"), 0, row);
        supplierCombo = new ComboBox<>();
        loadSuppliers();
        grid.add(supplierCombo, 1, row++);

        // Описание
        grid.add(new Label("Описание:"), 0, row);
        descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setPrefWidth(400);
        grid.add(descriptionArea, 1, row++);

        // Цена
        grid.add(new Label("Цена:"), 0, row);
        priceField = new TextField();
        grid.add(priceField, 1, row++);

        // Количество
        grid.add(new Label("Количество:"), 0, row);
        countField = new TextField();
        grid.add(countField, 1, row++);

        // Скидка
        grid.add(new Label("Скидка (%):"), 0, row);
        discountField = new TextField();
        grid.add(discountField, 1, row++);

        // Единица измерения
        grid.add(new Label("Ед. измерения:"), 0, row);
        unitField = new TextField();
        grid.add(unitField, 1, row++);

        // Кнопки
        Button saveBtn = new Button("Сохранить");
        saveBtn.setOnAction(e -> saveProduct());
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(10, saveBtn, cancelBtn);
        grid.add(buttons, 1, row);

        // Заполняем данными при редактировании
        if (editingProduct != null) {
            fillFields();
        }

        // Растягиваем поля (без цикла, без Node)
        // Наименование
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        nameField.setMaxWidth(Double.MAX_VALUE);

        // Описание
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);

        // Цена
        GridPane.setHgrow(priceField, Priority.ALWAYS);
        priceField.setMaxWidth(Double.MAX_VALUE);

        // Количество
        GridPane.setHgrow(countField, Priority.ALWAYS);
        countField.setMaxWidth(Double.MAX_VALUE);

        // Скидка
        GridPane.setHgrow(discountField, Priority.ALWAYS);
        discountField.setMaxWidth(Double.MAX_VALUE);

        // Единица измерения
        GridPane.setHgrow(unitField, Priority.ALWAYS);
        unitField.setMaxWidth(Double.MAX_VALUE);

        // Категория
        GridPane.setHgrow(categoryCombo, Priority.ALWAYS);
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        // Производитель
        GridPane.setHgrow(brandCombo, Priority.ALWAYS);
        brandCombo.setMaxWidth(Double.MAX_VALUE);

        // Поставщик
        GridPane.setHgrow(supplierCombo, Priority.ALWAYS);
        supplierCombo.setMaxWidth(Double.MAX_VALUE);

        Scene scene = new Scene(grid, 600, 650);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
    // Переносит данные из объекта editingProducts редактируемый товар в графические элементы интерфейса
    private void fillFields() {
        nameField.setText(editingProduct.getName());
        priceField.setText(String.valueOf(editingProduct.getPrice()));
        countField.setText(String.valueOf(editingProduct.getCount()));
        discountField.setText(String.valueOf(editingProduct.getDiscount()));
        unitField.setText(editingProduct.getUnit());
        descriptionArea.setText(editingProduct.getDescription());
        categoryCombo.setValue(editingProduct.getCategoryName());
        brandCombo.setValue(editingProduct.getBrandName());
        supplierCombo.setValue(editingProduct.getSupplierName());

        currentPhotoPath = editingProduct.getPhoto();
        if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                photoView.setImage(new Image(file.toURI().toString()));
            }
        }
    }

    private void uploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String projectDir = System.getProperty("user.dir");
                Path targetDir = Path.of(projectDir, "photos");
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                String fileName = System.currentTimeMillis() + "_" + file.getName();
                Path targetFile = targetDir.resolve(fileName);
                Files.copy(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
                currentPhotoPath = targetFile.toString();
                photoView.setImage(new Image(targetFile.toUri().toString()));
            } catch (IOException e) {
                showAlert("Ошибка", "Не удалось сохранить фото: " + e.getMessage());
            }
        }
    }

    private void loadCategories() {
        String sql = "SELECT name FROM categories";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categoryCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadBrands() {
        String sql = "SELECT name FROM brands";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                brandCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSuppliers() {
        String sql = "SELECT name FROM suppliers";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                supplierCombo.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Сохранение товаров
    private void saveProduct() {
        // Валидация
        if (nameField.getText().isEmpty()) {
            showAlert("Ошибка", "Введите наименование товара");
            return;
        }
        double price;
        int count, discount;
        try {
            price = Double.parseDouble(priceField.getText());
            if (price < 0) throw new NumberFormatException();
            count = Integer.parseInt(countField.getText());
            if (count < 0) throw new NumberFormatException();
            discount = Integer.parseInt(discountField.getText());
            if (discount < 0 || discount > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Цена, количество и скидка должны быть неотрицательными числами (скидка 0-100)");
            return;
        }
        // Подключение к БД и данные, которые берутся из БД
        try (Connection conn = getConnection()) {
            if (editingProduct == null) {
                // INSERT
                String sql = "INSERT INTO products (name, price, count, discount, photo, category_id, brand_id, supplier_id, description, unit) " +
                        "VALUES (?, ?, ?, ?, ?, (SELECT id FROM categories WHERE name = ?), " +
                        "(SELECT id FROM brands WHERE name = ?), (SELECT id FROM suppliers WHERE name = ?), ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nameField.getText());
                    pstmt.setDouble(2, price);
                    pstmt.setInt(3, count);
                    pstmt.setInt(4, discount);
                    pstmt.setString(5, currentPhotoPath);
                    pstmt.setString(6, categoryCombo.getValue());
                    pstmt.setString(7, brandCombo.getValue());
                    pstmt.setString(8, supplierCombo.getValue());
                    pstmt.setString(9, descriptionArea.getText());
                    pstmt.setString(10, unitField.getText());
                    pstmt.executeUpdate();
                }
            } else {
                // UPDATE
                String sql = "UPDATE products SET name=?, price=?, count=?, discount=?, photo=?, " +
                        "category_id=(SELECT id FROM categories WHERE name=?), " +
                        "brand_id=(SELECT id FROM brands WHERE name=?), " +
                        "supplier_id=(SELECT id FROM suppliers WHERE name=?), description=?, unit=? " +
                        "WHERE id=?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, nameField.getText());
                    pstmt.setDouble(2, price);
                    pstmt.setInt(3, count);
                    pstmt.setInt(4, discount);
                    pstmt.setString(5, currentPhotoPath);
                    pstmt.setString(6, categoryCombo.getValue());
                    pstmt.setString(7, brandCombo.getValue());
                    pstmt.setString(8, supplierCombo.getValue());
                    pstmt.setString(9, descriptionArea.getText());
                    pstmt.setString(10, unitField.getText());
                    pstmt.setInt(11, editingProduct.getId());
                    pstmt.executeUpdate();
                }
            }
            showAlert("Успех", "Товар сохранён");
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
    // Уведомления о каком-то событии действий пользователя
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
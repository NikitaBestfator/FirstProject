package org.example;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.sql.*;
import java.util.Optional;

public class MainView extends Application {

    private User currentUser;
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private TableView<Product> tableView = new TableView<>();
    private Stage primaryStage;

    public MainView(User user) {
        this.currentUser = user;
    }

    public MainView() {
        this.currentUser = new User(0, "guest", "", "Гость", "GUEST");
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Обувной магазин - Главное окно");

        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Иконка не загружена: " + e.getMessage());
        }

        loadProducts();
        createProductTable();

        // Создаём верхнюю панель (ФИО + Выйти + Заказы)
        HBox topBar = createTopBar();

        // Если роль MANAGER или ADMIN — добавляем панель фильтров
        VBox header = new VBox();
        if (currentUser.getRole().equals("MANAGER") || currentUser.getRole().equals("ADMIN")) {
            header.getChildren().addAll(topBar, createControlPanel());
        } else {
            header.getChildren().add(topBar);
        }

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(header);
        mainLayout.setCenter(tableView);

        // Кнопки для ADMIN (Добавить товар + Удалить товар)
        if (currentUser.getRole().equals("ADMIN")) {
            Button addButton = new Button("Добавить товар");
            addButton.setOnAction(e -> openProductForm(null));

            Button deleteButton = new Button("Удалить выбранный товар");
            deleteButton.setOnAction(e -> deleteSelectedProduct());

            HBox bottomBar = new HBox(10, addButton, deleteButton);
            bottomBar.setPadding(new Insets(10));
            bottomBar.setAlignment(Pos.CENTER_LEFT);
            mainLayout.setBottom(bottomBar);
        }

        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Загрузка товаров из БД
    private void loadProducts() {
        productList.clear();
        String sql = "SELECT p.id, p.name, p.price, p.count, p.discount, p.photo, " +
                "p.description, p.unit, " +
                "c.name AS categoryName, b.name AS brandName, s.name AS supplierName " +
                "FROM products p " +
                "LEFT JOIN categories c ON p.category_id = c.id " +
                "LEFT JOIN brands b ON p.brand_id = b.id " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.id";

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"), rs.getString("name"),
                        rs.getDouble("price"), rs.getInt("count"), rs.getInt("discount"),
                        rs.getString("photo"), rs.getString("categoryName"),
                        rs.getString("brandName"), rs.getString("supplierName"),
                        rs.getString("description"), rs.getString("unit")
                );
                productList.add(product);
            }
            tableView.setItems(productList);
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить товары: " + e.getMessage());
        }
    }

    // Создание таблицы товаров
    private void createProductTable() {
        tableView.getColumns().clear();

        // Колонка с фото
        TableColumn<Product, String> photoCol = new TableColumn<>("Фото");
        photoCol.setCellFactory(col -> new TableCell<Product, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String photoPath, boolean empty) {
                super.updateItem(photoPath, empty);
                if (empty || photoPath == null || photoPath.isEmpty()) {
                    setGraphic(null);
                    return;
                }
                try {
                    File file = new File(photoPath);
                    if (file.exists()) {
                        imageView.setImage(new Image(file.toURI().toString()));
                    } else {
                        imageView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
                    }
                } catch (Exception e) {
                    imageView.setImage(new Image(getClass().getResourceAsStream("/images/default.png")));
                }
                setGraphic(imageView);
            }
        });
        photoCol.setPrefWidth(80);

        TableColumn<Product, String> nameCol = new TableColumn<>("Наименование");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Product, String> categoryCol = new TableColumn<>("Категория");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));

        TableColumn<Product, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<Product, String> brandCol = new TableColumn<>("Производитель");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brandName"));

        TableColumn<Product, String> supplierCol = new TableColumn<>("Поставщик");
        supplierCol.setCellValueFactory(new PropertyValueFactory<>("supplierName"));

        TableColumn<Product, String> priceCol = new TableColumn<>("Цена");
        priceCol.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                Product p = getTableRow().getItem();
                double finalPrice = p.getPrice() * (1 - p.getDiscount() / 100.0);
                if (p.getDiscount() > 0) {
                    setText(String.format("%.2f руб.\n(скидка %d%%, итого: %.2f руб.)",
                            p.getPrice(), p.getDiscount(), finalPrice));
                } else {
                    setText(String.format("%.2f руб.", p.getPrice()));
                }
            }
        });
        priceCol.setPrefWidth(150);

        TableColumn<Product, Integer> countCol = new TableColumn<>("Количество");
        countCol.setCellValueFactory(new PropertyValueFactory<>("count"));

        TableColumn<Product, Integer> discountCol = new TableColumn<>("Скидка (%)");
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discount"));

        TableColumn<Product, String> unitCol = new TableColumn<>("Ед. изм.");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));

        tableView.getColumns().addAll(photoCol, nameCol, categoryCol, descCol, brandCol,
                supplierCol, priceCol, countCol, discountCol, unitCol);
        tableView.setItems(productList);

        // Подсветка строк
        tableView.setRowFactory(tv -> new TableRow<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);
                if (product == null || empty) {
                    setStyle("");
                    return;
                }
                if (product.getCount() == 0) {
                    setStyle("-fx-background-color: lightblue;");
                } else if (product.getDiscount() > 15) {
                    setStyle("-fx-background-color: #2E8B57;");
                } else {
                    setStyle("");
                }
            }
        });

        // Двойной клик для редактирования (только ADMIN)
        if (currentUser.getRole().equals("ADMIN")) {
            tableView.setRowFactory(tv -> {
                TableRow<Product> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !row.isEmpty()) {
                        openProductForm(row.getItem());
                    }
                });
                return row;
            });
        }
    }

    // Панель управления (поиск, фильтр, сортировка)
    private HBox createControlPanel() {
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск...");

        ComboBox<String> supplierFilter = new ComboBox<>();
        supplierFilter.getItems().add("Все поставщики");
        loadSuppliers(supplierFilter);
        supplierFilter.setValue("Все поставщики");

        ToggleGroup sortGroup = new ToggleGroup();
        RadioButton sortAsc = new RadioButton("По возрастанию остатка");
        RadioButton sortDesc = new RadioButton("По убыванию остатка");
        sortAsc.setToggleGroup(sortGroup);
        sortDesc.setToggleGroup(sortGroup);
        sortAsc.setSelected(true);

        // Обработчики
        searchField.textProperty().addListener((obs, old, newVal) ->
                applyFilters(searchField.getText(), supplierFilter.getValue(), sortGroup));
        supplierFilter.valueProperty().addListener((obs, old, newVal) ->
                applyFilters(searchField.getText(), newVal, sortGroup));
        sortGroup.selectedToggleProperty().addListener((obs, old, newVal) ->
                applyFilters(searchField.getText(), supplierFilter.getValue(), sortGroup));

        HBox controlPanel = new HBox(10, searchField, supplierFilter, sortAsc, sortDesc);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #e0e0e0;");
        return controlPanel;
    }

    // Загрузка поставщиков для фильтра
    private void loadSuppliers(ComboBox<String> comboBox) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM suppliers")) {
            while (rs.next()) {
                comboBox.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            showAlert("Ошибка", "Не удалось загрузить поставщиков: " + e.getMessage());
        }
    }

    // Применение фильтров и сортировки
    private void applyFilters(String searchText, String supplier, ToggleGroup sortGroup) {
        ObservableList<Product> filtered = FXCollections.observableArrayList();

        for (Product p : productList) {
            boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                    p.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchText.toLowerCase())) ||
                    (p.getBrandName() != null && p.getBrandName().toLowerCase().contains(searchText.toLowerCase()));

            boolean matchesSupplier = supplier == null || supplier.equals("Все поставщики") ||
                    (p.getSupplierName() != null && p.getSupplierName().equals(supplier));

            if (matchesSearch && matchesSupplier) {
                filtered.add(p);
            }
        }

        // Сортировка
        RadioButton selected = (RadioButton) sortGroup.getSelectedToggle();
        if (selected != null) {
            if (selected.getText().equals("По возрастанию остатка")) {
                filtered.sort((a, b) -> Integer.compare(a.getCount(), b.getCount()));
            } else if (selected.getText().equals("По убыванию остатка")) {
                filtered.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
            }
        }

        tableView.setItems(filtered);
    }

    // Верхняя панель: ФИО пользователя, кнопка выхода, кнопка "Заказы"
    private HBox createTopBar() {
        Label userNameLabel = new Label(currentUser.getName() + " (" + currentUser.getRole() + ")");
        userNameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10;");

        Button logoutButton = new Button("Выйти");
        logoutButton.setOnAction(e -> {
            primaryStage.close();
            LoginView loginView = new LoginView();
            Stage loginStage = new Stage();
            loginView.start(loginStage);
        });

        HBox topBar = new HBox(20, userNameLabel, logoutButton);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #f0f0f0;");

        // Кнопка "Заказы" для MANAGER и ADMIN
        if (currentUser.getRole().equals("MANAGER") || currentUser.getRole().equals("ADMIN")) {
            Button ordersButton = new Button("Заказы");
            ordersButton.setOnAction(e -> {
                primaryStage.close();
                OrdersView ordersView = new OrdersView(currentUser);
                Stage ordersStage = new Stage();
                ordersView.start(ordersStage);
            });
            topBar.getChildren().add(0, ordersButton);
        }

        return topBar;
    }

    // Форма добавления/редактирования товара
    private void openProductForm(Product product) {
        new ProductForm(product, () -> {
            loadProducts();
            tableView.refresh();
        });
    }

    // Удаление выбранного товара (только для ADMIN)
    private void deleteSelectedProduct() {
        Product selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Внимание", "Сначала выберите товар для удаления");
            return;
        }

        // Проверяем, есть ли товар в заказах
        String checkSql = "SELECT COUNT(*) FROM products_orders WHERE product_id = ?";
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/shoe_shop?useSSL=false&serverTimezone=UTC",
                "admin", "admin123");
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {

            pstmt.setInt(1, selected.getId());
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                showAlert("Ошибка", "Нельзя удалить товар, который присутствует в заказах");
                return;
            }

            // Подтверждение удаления
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение");
            confirm.setHeaderText(null);
            confirm.setContentText("Вы уверены, что хотите удалить товар \"" + selected.getName() + "\"?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            // Удаляем из БД
            String deleteSql = "DELETE FROM products WHERE id = ?";
            try (PreparedStatement pstmt2 = conn.prepareStatement(deleteSql)) {
                pstmt2.setInt(1, selected.getId());
                pstmt2.executeUpdate();
            }

            // Удаляем фото (если есть)
            if (selected.getPhoto() != null && !selected.getPhoto().isEmpty()) {
                File photoFile = new File(selected.getPhoto());
                if (photoFile.exists()) {
                    photoFile.delete();
                }
            }

            showAlert("Успех", "Товар удалён");
            loadProducts();

        } catch (SQLException e) {
            showAlert("Ошибка БД", e.getMessage());
        }
    }

    // Уведомления
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
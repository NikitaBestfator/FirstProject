-- ------------------------------------------------------------
-- 2. Таблица пользователей
-- ------------------------------------------------------------
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,   -- хранить хэш (bcrypt)
    name VARCHAR(100) NOT NULL,
    role ENUM('GUEST', 'CLIENT', 'MANAGER', 'ADMIN') DEFAULT 'CLIENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 3. Таблица категорий
-- ------------------------------------------------------------
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- 4. Таблица брендов (производители)
-- ------------------------------------------------------------
CREATE TABLE brands (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- 5. Таблица поставщиков
-- ------------------------------------------------------------
CREATE TABLE suppliers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- 6. Таблица товаров (с фото)
-- ------------------------------------------------------------
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    count INT NOT NULL DEFAULT 0 CHECK (count >= 0),
    discount INT DEFAULT 0 CHECK (discount BETWEEN 0 AND 100),
    unit VARCHAR(20) DEFAULT 'шт.',
    photo VARCHAR(255),   -- путь к файлу изображения (например, /images/product_123.jpg)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    category_id INT,
    brand_id INT,
    supplier_id INT,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (brand_id) REFERENCES brands(id) ON DELETE RESTRICT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- 7. Таблица заказов (шапка)
-- ------------------------------------------------------------
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    total_cost DECIMAL(10,2) DEFAULT 0.00,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('NEW', 'PAID', 'CANCELED') DEFAULT 'NEW',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- ------------------------------------------------------------
-- 8. Таблица содержимого заказа (products_orders)
-- ------------------------------------------------------------
CREATE TABLE products_orders (
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_count INT NOT NULL CHECK (product_count > 0),
    price_at_moment DECIMAL(10,2) NOT NULL CHECK (price_at_moment >= 0),
    PRIMARY KEY (order_id, product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- Insert-запросы
-- категории
INSERT INTO categories (name) VALUES 
('Кроссовки'), 
('Ботинки'), 
('Сандалии'), 
('Туфли'), 
('Кеды');

-- бренды (производители)
INSERT INTO brands (name) VALUES 
('Nike'), 
('Adidas'), 
('Reebok'), 
('Puma'), 
('New Balance');

-- поставщики
INSERT INTO suppliers (name) VALUES 
('ООО СпортМастер'),
('ИП СпортТрейд'),
('ТД Обувной Мир'),
('ООО МегаСпорт');

INSERT INTO users (login, password, name, role) VALUES 
('admin', 'admin123', 'Алексей Иванов', 'ADMIN'),
('manager', 'manager123', 'Мария Петрова', 'MANAGER'),
('client', 'client123', 'Дмитрий Сидоров', 'CLIENT'),
('guest', 'guest', 'Гость', 'GUEST');

INSERT INTO products 
(name, description, price, count, discount, unit, photo, category_id, brand_id, supplier_id) 
VALUES 

-- Кроссовки (категория 1)
('Nike Air Max 90', 
 'Классические кроссовки с амортизацией Air Max', 
 8990.00, 25, 0, 'пара', '/images/products/nike_air_max_90.jpg', 1, 1, 1),

('Nike Revolution 6', 
 'Легкие кроссовки для бега', 
 4990.00, 45, 10, 'пара', '/images/products/nike_revolution.jpg', 1, 1, 1),

('Adidas Ultraboost 22', 
 'Беговые кроссовки с максимальной энергией возврата', 
 12990.00, 12, 15, 'пара', '/images/products/adidas_ultraboost.jpg', 1, 2, 2),

('Adidas Superstar', 
 'Знаменитые кеды-ракушки', 
 7990.00, 8, 20, 'пара', '/images/products/adidas_superstar.jpg', 1, 2, 2),

('Puma RS-X', 
 'Массивные кроссовки в ретро-стиле', 
 6990.00, 30, 0, 'пара', '/images/products/puma_rsx.jpg', 1, 4, 1),

('New Balance 574', 
 'Классические кроссовки N-логи', 
 6490.00, 18, 5, 'пара', '/images/products/nb_574.jpg', 1, 5, 3),

-- Ботинки (категория 2)
('Timberland Classic', 
 'Знаменитые жёлтые ботинки', 
 15990.00, 7, 0, 'пара', '/images/products/timberland.jpg', 2, 1, 1),

('Кожаные ботинки Dr. Martens', 
 'Ботинки на толстой подошве', 
 13990.00, 0, 25, 'пара', '/images/products/dr_martens.jpg', 2, 2, 2),

-- Сандалии (категория 3)
('Teva Hurricane XLT2', 
 'Треккинговые сандалии', 
 4990.00, 22, 0, 'пара', '/images/products/teva_hurricane.jpg', 3, 3, 3),

('Crocs Classic', 
 'Легкие сандалии-кроксы', 
 3490.00, 60, 30, 'пара', '/images/products/crocs.jpg', 3, 4, 1),

-- Туфли (категория 4)
('Кожаные туфли Ecco', 
 'Классические офисные туфли', 
 11990.00, 14, 0, 'пара', '/images/products/ecco.jpg', 4, 1, 2),

('Лоферы Geox', 
 'Дышащие туфли без шнурков', 
 8990.00, 9, 10, 'пара', '/images/products/geox.jpg', 4, 2, 3),

-- Кеды (категория 5)
('Converse Chuck Taylor', 
 'Классические высокие кеды', 
 5990.00, 32, 15, 'пара', '/images/products/converse.jpg', 5, 3, 1),

('Vans Old Skool', 
 'Знаменитые кеды с полосой', 
 5490.00, 27, 0, 'пара', '/images/products/vans.jpg', 5, 4, 2);

select count(*) FROM products;

-- Создаем пользователя для приложения
CREATE USER 'app_user'@'localhost' IDENTIFIED BY 'strong_password';

GRANT ALL PRIVILEGES ON shoe_shop.* TO 'app_user'@'localhost';

-- Применить изменения
FLUSH PRIVILEGES;

-- Проверить права пользователя 'app_user'
SHOW GRANTS FOR 'app_user'@'localhost';

DROP USER 'app_user'@'localhost';

-- 1. Создаем пользователя 'admin' с паролем 'admin123'
CREATE USER 'admin'@'localhost' IDENTIFIED BY 'admin123';

-- 2. Даем ему полный доступ к вашей базе данных 'shoe_shop'
GRANT ALL PRIVILEGES ON shoe_shop.* TO 'admin'@'localhost';

-- 3. Обновляем привилегии, чтобы изменения вступили в силу
FLUSH PRIVILEGES;

SET FOREIGN_KEY_CHECKS = 0;  -- временно отключаем проверку ключей

TRUNCATE TABLE products_orders;
TRUNCATE TABLE orders;
TRUNCATE TABLE products;
TRUNCATE TABLE users;
TRUNCATE TABLE categories;
TRUNCATE TABLE brands;
TRUNCATE TABLE suppliers;

SET FOREIGN_KEY_CHECKS = 1;  -- включаем обратно

SELECT id, name, discount FROM products;

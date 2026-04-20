-- =============================================
-- DATABASE: Digital Account Marketplace
-- Engine: MySQL 8.x
-- Date: 2026-03-08
-- =============================================

DROP DATABASE IF EXISTS digital_marketplace;
CREATE DATABASE digital_marketplace CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE digital_marketplace;

-- =============================================
-- 1. USERS (Khách hàng + Admin)
-- =============================================
CREATE TABLE users (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(100)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    role            ENUM('customer', 'admin') NOT NULL DEFAULT 'customer',
    avatar_url      VARCHAR(500)    DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB;

-- =============================================
-- 2. CATEGORIES (Danh mục sản phẩm)
-- =============================================
CREATE TABLE categories (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    slug            VARCHAR(120)    NOT NULL UNIQUE,
    icon_url        VARCHAR(500)    DEFAULT NULL,
    description     TEXT            DEFAULT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    sort_order      INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_categories_slug (slug)
) ENGINE=InnoDB;

-- =============================================
-- 3. PRODUCTS (Sản phẩm: Netflix, Canva, ...)
-- =============================================
CREATE TABLE products (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    category_id     INT             NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    slug            VARCHAR(220)    NOT NULL UNIQUE,
    description     TEXT            DEFAULT NULL,
    image_url       VARCHAR(500)    DEFAULT NULL,
    original_price  DECIMAL(12,0)   NOT NULL DEFAULT 0,
    sale_price      DECIMAL(12,0)   NOT NULL DEFAULT 0,
    delivery_type   ENUM('key', 'account', 'both') NOT NULL DEFAULT 'account',
    warranty_info   VARCHAR(255)    DEFAULT NULL,
    is_featured     TINYINT(1)      NOT NULL DEFAULT 0,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    stock_count     INT             NOT NULL DEFAULT 0,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_products_category (category_id),
    INDEX idx_products_slug (slug),
    INDEX idx_products_featured (is_featured),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================
-- 4. PRODUCT_KEYS (Kho key/serial hoặc account)
-- =============================================
CREATE TABLE product_keys (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    product_id       INT             NOT NULL,
    key_type         ENUM('serial_key', 'account') NOT NULL DEFAULT 'account',
    serial_key       VARCHAR(500)    DEFAULT NULL,
    account_email    VARCHAR(200)    DEFAULT NULL,
    account_password VARCHAR(200)    DEFAULT NULL,
    status           ENUM('available', 'sold', 'reserved') NOT NULL DEFAULT 'available',
    sold_at          DATETIME        DEFAULT NULL,
    order_item_id    INT             DEFAULT NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_pk_product (product_id),
    INDEX idx_pk_status (status),
    CONSTRAINT fk_pk_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =============================================
-- 5. COUPONS (Mã giảm giá đơn giản)
-- =============================================
CREATE TABLE coupons (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL UNIQUE,
    discount_type   ENUM('percent', 'fixed') NOT NULL DEFAULT 'percent',
    discount_value  DECIMAL(12,0)   NOT NULL DEFAULT 0,
    min_order_amount DECIMAL(12,0)  NOT NULL DEFAULT 0,
    max_uses        INT             NOT NULL DEFAULT 100,
    used_count      INT             NOT NULL DEFAULT 0,
    start_date      DATE            DEFAULT NULL,
    end_date        DATE            DEFAULT NULL,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_coupons_code (code)
) ENGINE=InnoDB;

-- =============================================
-- 6. ORDERS (Đơn hàng - Header)
-- =============================================
CREATE TABLE orders (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    coupon_id       INT             DEFAULT NULL,
    total_amount    DECIMAL(12,0)   NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,0)   NOT NULL DEFAULT 0,
    final_amount    DECIMAL(12,0)   NOT NULL DEFAULT 0,
    status          ENUM('pending', 'paid', 'completed', 'cancelled', 'refunded') NOT NULL DEFAULT 'pending',
    payment_method  VARCHAR(50)     DEFAULT NULL,
    note            TEXT            DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- =============================================
-- 7. ORDER_ITEMS (Chi tiết đơn hàng)
-- =============================================
CREATE TABLE order_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    order_id        INT             NOT NULL,
    product_id      INT             NOT NULL,
    product_key_id  INT             DEFAULT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12,0)   NOT NULL DEFAULT 0,
    subtotal        DECIMAL(12,0)   NOT NULL DEFAULT 0,

    INDEX idx_oi_order (order_id),
    INDEX idx_oi_product (product_id),
    CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_key FOREIGN KEY (product_key_id) REFERENCES product_keys(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- Liên kết ngược product_keys.order_item_id -> order_items.id
ALTER TABLE product_keys
ADD CONSTRAINT fk_pk_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL;

-- =============================================
-- 8. REVIEWS (Đánh giá sản phẩm)
-- =============================================
CREATE TABLE reviews (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT             NOT NULL,
    product_id      INT             NOT NULL,
    rating          TINYINT         NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT            DEFAULT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_reviews_product (product_id),
    INDEX idx_reviews_user (user_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- =============================================================
-- SAMPLE DATA (Dữ liệu mẫu)
-- =============================================================

-- Admin & Khách
INSERT INTO users (username, email, password_hash, full_name, phone, role) VALUES
('admin',       'admin@digitalstore.vn',    '$2y$10$hashedpasswordhere',  'Quản Trị Viên',    '0901234567',   'admin'),
('nguyenhoang', 'hoang@gmail.com',          '$2y$10$hashedpasswordhere',  'Nguyễn Hoàng',     '0912345678',   'customer'),
('trananh',     'trananh92@gmail.com',      '$2y$10$hashedpasswordhere',  'Trần Anh',         '0923456789',   'customer'),
('linhchi',     'linhchi@yahoo.com',         '$2y$10$hashedpasswordhere',  'Linh Chi',         '0934567890',   'customer');

-- Danh mục
INSERT INTO categories (name, slug, icon_url, description, sort_order) VALUES
('Giải Trí & Phim Ảnh',    'giai-tri-phim-anh',    'https://img.icons8.com/color/48/netflix-desktop-app.png',  'Netflix, Disney+, HBO...', 1),
('Âm Nhạc',                'am-nhac',              'https://img.icons8.com/color/48/spotify--v1.png',           'Spotify, Apple Music...',  2),
('Thiết Kế & Làm Việc',    'thiet-ke-lam-viec',    'https://img.icons8.com/fluency/48/canva.png',              'Canva, Adobe, Zoom...',    3),
('Giáo Dục & Học Tập',     'giao-duc-hoc-tap',     'https://img.icons8.com/fluency/48/duolingo-logo.png',      'Duolingo, Elsa, Quizlet',  4),
('Tài Khoản Game',         'tai-khoan-game',        'https://img.icons8.com/color/48/steam.png',               'Steam, Xbox, PS Store',    5),
('Công Nghệ & AI',         'cong-nghe-ai',          'https://img.icons8.com/fluency/48/chatgpt.png',           'ChatGPT, Gemini, Copilot', 6);

-- Sản phẩm
INSERT INTO products (category_id, name, slug, description, image_url, original_price, sale_price, delivery_type, warranty_info, is_featured, stock_count) VALUES
(1, 'Tài Khoản Netflix Premium 4K - 1 Tháng',    'netflix-premium-4k-1-thang',     'Cấp sẵn profile riêng. Xem trên Smart TV, PC, điện thoại.', 'https://img.icons8.com/color/150/netflix-desktop-app.png', 260000, 99000, 'account', 'Bảo hành 1 tháng', 1, 50),
(1, 'Tài Khoản Disney+ Premium 1 Tháng',         'disney-plus-premium-1-thang',    'Xem phim Marvel, Star Wars, Pixar chất lượng 4K.',           'https://img.icons8.com/color/150/disney-plus.png',          180000, 79000, 'account', 'Bảo hành 30 ngày', 0, 30),
(2, 'Spotify Premium 1 Năm - Đăng Ký Trực Tiếp', 'spotify-premium-1-nam',          'Nghe nhạc không quảng cáo. Chất lượng cao nhất.',            'https://img.icons8.com/color/150/spotify--v1.png',          500000, 249000, 'account', 'Bảo hành 12 tháng', 1, 40),
(2, 'Youtube Premium 6 Tháng',                    'youtube-premium-6-thang',        'Nâng cấp chính chủ email. Không quảng cáo trên mọi thiết bị.', 'https://img.icons8.com/color/150/youtube-play.png',       300000, 169000, 'account', 'Bảo hành 6 tháng', 0, 35),
(3, 'Tài Khoản Canva Pro Vĩnh Viễn',             'canva-pro-vinh-vien',            'Nâng cấp email chính chủ. Mở khóa toàn bộ tính năng.',       'https://img.icons8.com/fluency/150/canva.png',             550000, 99000, 'account', 'Vĩnh viễn', 1, 100),
(3, 'Adobe Creative Cloud 1 Năm',                 'adobe-creative-cloud-1-nam',     'Photoshop, Illustrator, Premiere Pro, After Effects...',      'https://img.icons8.com/color/150/adobe-creative-cloud.png', 2000000, 890000, 'key', 'Bảo hành 12 tháng', 0, 15),
(4, 'Duolingo Super (Plus) 1 Năm',                'duolingo-super-1-nam',           'Học ngoại ngữ không giới hạn lượt sai, không quảng cáo.',     'https://img.icons8.com/color/150/duolingo-logo.png',       600000, 140000, 'account', 'Bảo hành 12 tháng', 0, 25),
(5, 'Steam Key - GTA V Premium Edition',          'steam-key-gta-v-premium',        'Key kích hoạt game trên Steam. Toàn cầu, vĩnh viễn.',        'https://img.icons8.com/color/150/steam.png',               500000, 199000, 'key', 'Key vĩnh viễn', 1, 20),
(6, 'Tài Khoản ChatGPT Plus 1 Tháng',             'chatgpt-plus-1-thang',           'Trải nghiệm GPT-4 thông minh nhất. Không giới hạn.',         'https://img.icons8.com/fluency/150/chatgpt.png',           450000, 190000, 'account', 'Bảo hành 30 ngày', 1, 60);

-- Kho tài khoản & key mẫu  
INSERT INTO product_keys (product_id, key_type, account_email, account_password, status) VALUES
(1, 'account', 'nf_user01@proton.me',    'Nf@Secure2026!',    'available'),
(1, 'account', 'nf_user02@proton.me',    'Nf@Premium88!',     'available'),
(1, 'account', 'nf_user03@proton.me',    'Nf@Watch4K!',       'sold'),
(3, 'account', 'sp_music01@proton.me',   'Sp@Song2026!',      'available'),
(5, 'account', 'canva_pro01@proton.me',  'Cv@Design99!',      'available'),
(5, 'account', 'canva_pro02@proton.me',  'Cv@Creative!',      'available');

INSERT INTO product_keys (product_id, key_type, serial_key, status) VALUES
(6, 'serial_key', 'ADBE-XXXX-YYYY-ZZZZ-1111',  'available'),
(6, 'serial_key', 'ADBE-AAAA-BBBB-CCCC-2222',  'available'),
(8, 'serial_key', 'STEAM-GTA5-RRRR-TTTT-0001', 'available'),
(8, 'serial_key', 'STEAM-GTA5-SSSS-UUUU-0002', 'available'),
(8, 'serial_key', 'STEAM-GTA5-VVVV-WWWW-0003', 'sold');

-- Mã giảm giá mẫu
INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, max_uses, start_date, end_date) VALUES
('WELCOME10',  'percent',  10,     0,       500,    '2026-01-01', '2026-12-31'),
('SAVE50K',    'fixed',    50000,  200000,  100,    '2026-03-01', '2026-06-30'),
('VIP20',      'percent',  20,     500000,  50,     '2026-03-01', '2026-04-30');

-- Đơn hàng mẫu
INSERT INTO orders (user_id, coupon_id, total_amount, discount_amount, final_amount, status, payment_method) VALUES
(2, 1, 99000,  9900,   89100,  'completed', 'MoMo'),
(3, NULL, 249000, 0,    249000, 'completed', 'Banking'),
(4, 2, 348000, 50000,  298000, 'paid',      'ZaloPay'),
(2, NULL, 190000, 0,    190000, 'pending',   NULL);

-- Chi tiết đơn hàng
INSERT INTO order_items (order_id, product_id, product_key_id, quantity, unit_price, subtotal) VALUES
(1, 1, 3, 1, 99000,  99000),
(2, 3, 4, 1, 249000, 249000),
(3, 1, NULL, 1, 99000, 99000),
(3, 3, NULL, 1, 249000, 249000),
(4, 9, NULL, 1, 190000, 190000);

-- Đánh giá mẫu
INSERT INTO reviews (user_id, product_id, rating, comment) VALUES
(2, 1, 5, 'Tài khoản chạy ổn định, profile riêng, rất hài lòng!'),
(3, 3, 4, 'Spotify nghe nhạc mượt. Bảo hành nhanh khi có lỗi.'),
(4, 5, 5, 'Canva Pro vĩnh viễn, dùng cực thoải mái để thiết kế.'),
(2, 9, 5, 'ChatGPT Plus quá xịn, GPT-4 trả lời nhanh, chính xác.');

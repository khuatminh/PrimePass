-- Migration: Thêm bảng product_variants còn thiếu
-- Chạy lệnh này trên VPS nếu database đã được khởi tạo trước đó
-- docker exec -i marketplace-db mysql -u marketplace -p<password> digital_marketplace < database/migration_add_product_variants.sql

CREATE TABLE IF NOT EXISTS product_variants (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    product_id      INT             NOT NULL,
    variant_label   VARCHAR(200)    NOT NULL,
    price           BIGINT          NOT NULL,
    stock_count     INT             NOT NULL DEFAULT 0,
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,

    INDEX idx_pv_product (product_id),
    CONSTRAINT fk_pv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Thêm dữ liệu mẫu cho các sản phẩm hiện có (tùy chọn)
INSERT IGNORE INTO product_variants (product_id, variant_label, price, stock_count, is_active)
SELECT id, '1 Tháng', sale_price, stock_count, 1 FROM products WHERE is_active = 1;
-- Migration: Bổ sung cột variant_id + index + FK cho product_keys
-- An toàn để chạy lại nhiều lần (idempotent): tự bỏ qua nếu đã tồn tại.
-- Lý do: Hibernate auto-DDL có thể đã tạo cột variant_id nhưng KHÔNG tạo
-- index và FK, nên cần migration này để bổ sung phần còn thiếu.
--
-- Chạy:
-- docker exec -i marketplace-db mysql -u marketplace -p<password> digital_marketplace \
--   < database/migration_add_variant_id_to_product_keys.sql

-- 1) Cột variant_id (chỉ thêm nếu chưa có)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'product_keys'
      AND COLUMN_NAME  = 'variant_id'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE product_keys ADD COLUMN variant_id INT DEFAULT NULL AFTER product_id',
    'SELECT "Column variant_id already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Index idx_pk_variant (chỉ thêm nếu chưa có)
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'product_keys'
      AND INDEX_NAME   = 'idx_pk_variant'
);
SET @sql := IF(@idx_exists = 0,
    'ALTER TABLE product_keys ADD INDEX idx_pk_variant (variant_id)',
    'SELECT "Index idx_pk_variant already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) Foreign key fk_pk_variant -> product_variants(id) (chỉ thêm nếu chưa có)
SET @fk_exists := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA   = DATABASE()
      AND TABLE_NAME     = 'product_keys'
      AND CONSTRAINT_NAME = 'fk_pk_variant'
);
SET @sql := IF(@fk_exists = 0,
    'ALTER TABLE product_keys
        ADD CONSTRAINT fk_pk_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL',
    'SELECT "FK fk_pk_variant already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

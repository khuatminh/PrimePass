-- Synchronize order/payment columns with the JPA entities.
-- Safe to run repeatedly: every column, index, and constraint is guarded.
-- Run from the project directory:
-- docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
--   < database/migration_sync_order_schema.sql

SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'vnpay_txn_ref'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE orders ADD COLUMN vnpay_txn_ref VARCHAR(50) DEFAULT NULL AFTER note',
    'SELECT "Column orders.vnpay_txn_ref already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'vnpay_transaction_id'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE orders ADD COLUMN vnpay_transaction_id VARCHAR(50) DEFAULT NULL AFTER vnpay_txn_ref',
    'SELECT "Column orders.vnpay_transaction_id already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'uk_orders_vnpay_txn_ref'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE orders ADD UNIQUE INDEX uk_orders_vnpay_txn_ref (vnpay_txn_ref)',
    'SELECT "Index uk_orders_vnpay_txn_ref already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_vnpay_transaction_id'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE orders ADD INDEX idx_orders_vnpay_transaction_id (vnpay_transaction_id)',
    'SELECT "Index idx_orders_vnpay_transaction_id already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items'
      AND COLUMN_NAME = 'variant_id'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE order_items ADD COLUMN variant_id INT DEFAULT NULL AFTER product_id',
    'SELECT "Column order_items.variant_id already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items'
      AND INDEX_NAME = 'idx_oi_variant'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE order_items ADD INDEX idx_oi_variant (variant_id)',
    'SELECT "Index idx_oi_variant already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items'
      AND CONSTRAINT_NAME = 'fk_oi_variant'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql := IF(@exists = 0,
    'ALTER TABLE order_items ADD CONSTRAINT fk_oi_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE SET NULL',
    'SELECT "Constraint fk_oi_variant already exists" AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
